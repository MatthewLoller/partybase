/*
 * Copyright (c) 2026, Matthew Loller <matthewloller@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.matthewloller.partybase.party;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.matthewloller.partybase.PartybaseConfig;
import com.matthewloller.partybase.party.events.PartybaseMemberAvatar;
import com.matthewloller.partybase.party.events.PartybaseMemberData;
import com.matthewloller.partybase.party.events.PartybasePartyChanged;
import com.matthewloller.partybase.party.events.PartybaseSpecialCounter;
import com.matthewloller.partybase.party.events.PartybaseUserJoin;
import com.matthewloller.partybase.party.events.PartybaseUserPart;
import com.matthewloller.partybase.party.events.PartybaseUserSync;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PartyMemberAvatar;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.party.messages.PartyChatMessage;
import net.runelite.client.party.messages.PartyData;
import net.runelite.client.party.messages.PartyMemberMessage;
import net.runelite.client.party.messages.UserSync;
import net.runelite.client.plugins.specialcounter.SpecialCounterUpdate;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * The party Partybase talks to. With no server URL set it is RuneLite's own party: calls go to PartyService and
 * RuneLite's party events are re-posted as Partybase events. With a server URL set (Party server settings) it is a
 * Partybase relay server: one websocket, authenticated with the user's key, that carries every Partybase message
 * plus the member data (stats, inventory, gear, prayers) RuneLite's PartyDataService would otherwise send.
 *
 * Partybase code only uses this class and the Partybase party events, never PartyService or RuneLite's party
 * events, so both modes look the same to it. Partybase message classes arrive on the event bus in both modes.
 */
@Slf4j
@Singleton
public class PartyLink
{
	/** Only these schemes are accepted; plain ws:// only for a server on this machine. */
	static final String SECURE_SCHEME = "wss://";
	private static final long KEEPALIVE_SECONDS = 4 * 60;
	private static final long MAX_RETRY_SECONDS = 60;
	/** API Gateway frames are limited to 32 KB. */
	private static final int MAX_FRAME_BYTES = 30 * 1024;
	private static final int MAX_CHAT_LENGTH = 150;
	private static final String SYNC_TYPE = "UserSync";
	private static final String DATA_TYPE = "PartyData";
	private static final String CHAT_TYPE = "PartyChatMessage";
	private static final String SPEC_TYPE = "SpecialCounterUpdate";

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final PartyService partyService;
	private final WSClient wsClient;
	private final OkHttpClient httpClient;
	private final Gson gson;
	private final ScheduledExecutorService executor;
	private final ChatMessageManager chat;
	private final PartybaseConfig config;
	private final RelayDataSender dataSender;
	private final SecureRandom random = new SecureRandom();

	/** Message types by simple name, as registerMessage was called; used to decode relay traffic. */
	private final Map<String, Class<? extends PartyMemberMessage>> types = new ConcurrentHashMap<>();
	/** RuneLite party members wrapped once, so the same member is the same object. */
	private final Map<Long, RuneLiteMember> wrapped = new ConcurrentHashMap<>();

	// ---- relay state; generation guards against callbacks from an old socket ----
	private final Map<Long, RelayMember> relayMembers = new ConcurrentHashMap<>();
	private volatile String serverUrl;
	private volatile String passphrase;
	private volatile long partyId;
	private volatile long localMemberId;
	private volatile boolean joined;
	private volatile WebSocket socket;
	private volatile int generation;
	private volatile int retrySeconds = 2;
	private volatile String localName = "";
	private ScheduledFuture<?> keepalive;
	private ScheduledFuture<?> retry;

	@Inject
	PartyLink(Client client, ClientThread clientThread, EventBus eventBus, PartyService partyService,
		WSClient wsClient, OkHttpClient httpClient, Gson gson, ScheduledExecutorService executor,
		ChatMessageManager chat, PartybaseConfig config)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.partyService = partyService;
		this.wsClient = wsClient;
		this.httpClient = httpClient;
		this.gson = gson;
		this.executor = executor;
		this.chat = chat;
		this.config = config;
		this.dataSender = new RelayDataSender(client);
	}

	/** Called by PartybasePlugin before the modules start; registers this on the event bus. */
	public void startUp()
	{
		serverUrl = validServerUrl(config.serverUrl());
		registerMessage(UserSync.class);
		registerMessage(PartyData.class);
		registerMessage(PartyChatMessage.class);
		registerMessage(SpecialCounterUpdate.class);
		eventBus.register(this);
		eventBus.register(dataSender);
	}

	/** Called by PartybasePlugin after the modules stopped. Leaves a relay party; RuneLite's party is untouched. */
	public void shutDown()
	{
		eventBus.unregister(dataSender);
		eventBus.unregister(this);
		if (isRelay())
		{
			closeRelay();
			passphrase = null;
		}
		types.clear();
		wrapped.clear();
	}

	// ---- the party API Partybase uses ----

	/** A relay server is set; otherwise this is RuneLite's party. */
	public boolean isRelay()
	{
		return serverUrl != null;
	}

	/** The relay connection is up and joined (always true for RuneLite's party while in one). */
	public boolean isConnected()
	{
		return isRelay() ? joined : partyService.isInParty();
	}

	public boolean isInParty()
	{
		return isRelay() ? passphrase != null : partyService.isInParty();
	}

	public String getPartyPassphrase()
	{
		return isRelay() ? passphrase : partyService.getPartyPassphrase();
	}

	public long getPartyId()
	{
		return isRelay() ? partyId : partyService.getPartyId();
	}

	public PartybaseMember getLocalMember()
	{
		if (isRelay())
		{
			return joined ? relayMembers.get(localMemberId) : null;
		}
		return wrap(partyService.getLocalMember());
	}

	public PartybaseMember getMemberById(long memberId)
	{
		return isRelay() ? relayMembers.get(memberId) : wrap(partyService.getMemberById(memberId));
	}

	public List<PartybaseMember> getMembers()
	{
		List<PartybaseMember> out = new ArrayList<>();
		if (isRelay())
		{
			out.addAll(relayMembers.values());
		}
		else
		{
			for (PartyMember m : partyService.getMembers())
			{
				out.add(wrap(m));
			}
		}
		return out;
	}

	/** Client thread (it reads item names for the words). */
	public String generatePassphrase()
	{
		return partyService.generatePassphrase();
	}

	/** Joins a party by passphrase, or leaves with null. */
	public void changeParty(String newPassphrase)
	{
		if (!isRelay())
		{
			partyService.changeParty(newPassphrase);
			return;
		}
		closeRelay();
		passphrase = newPassphrase;
		partyId = newPassphrase == null ? 0 : passphraseToId(newPassphrase);
		// a new member id per party, kept over reconnects so leader and join times survive them
		localMemberId = newPassphrase == null ? 0 : randomMemberId();
		eventBus.post(new PartybasePartyChanged(newPassphrase, newPassphrase == null ? null : partyId));
		if (newPassphrase != null)
		{
			openRelay();
		}
	}

	/** Registers a message type for receiving (and on RuneLite's party, for WSClient to decode it). */
	public void registerMessage(Class<? extends PartyMemberMessage> type)
	{
		types.put(type.getSimpleName(), type);
		if (!isRelay() || isRuneLiteType(type))
		{
			wsClient.registerMessage(type);
		}
	}

	public void unregisterMessage(Class<? extends PartyMemberMessage> type)
	{
		types.remove(type.getSimpleName());
		if (!isRuneLiteType(type))
		{
			wsClient.unregisterMessage(type);
		}
	}

	/** RuneLite's own message types stay registered with WSClient: core plugins use them too. */
	private static boolean isRuneLiteType(Class<?> type)
	{
		return type.getName().startsWith("net.runelite.");
	}

	/**
	 * On a relay, RuneLite's party (if you are also in one) must not deliver Partybase messages, so Partybase's own
	 * types are only registered with WSClient on RuneLite's party.
	 */
	private void syncRuneLiteRegistrations()
	{
		for (Class<? extends PartyMemberMessage> type : types.values())
		{
			if (isRuneLiteType(type))
			{
				continue;
			}
			if (isRelay())
			{
				wsClient.unregisterMessage(type);
			}
			else
			{
				wsClient.registerMessage(type);
			}
		}
	}

	/** Sends to the whole party, you included. Dropped while a relay is reconnecting. */
	public void send(PartyMemberMessage message)
	{
		if (!isRelay())
		{
			partyService.send(message);
			return;
		}
		WebSocket ws = socket;
		if (ws == null || !joined)
		{
			return;
		}
		JsonObject envelope = new JsonObject();
		envelope.addProperty("t", message.getClass().getSimpleName());
		envelope.add("d", gson.toJsonTree(message));
		JsonObject frame = new JsonObject();
		frame.addProperty("action", "send");
		frame.add("data", envelope);
		String text = gson.toJson(frame);
		if (text.getBytes(StandardCharsets.UTF_8).length > MAX_FRAME_BYTES)
		{
			log.warn("Partybase relay: {} is too large to send ({} bytes)", envelope.get("t"), text.length());
			return;
		}
		ws.send(text);
	}

	// ---- settings ----

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!PartybaseConfig.GROUP.equals(e.getGroup())
			|| !(PartybaseConfig.SERVER_URL_KEY.equals(e.getKey()) || PartybaseConfig.SERVER_KEY_KEY.equals(e.getKey())))
		{
			return;
		}
		// switch transports and take the current party along
		String current = getPartyPassphrase();
		if (current != null)
		{
			changeParty(null);
		}
		serverUrl = validServerUrl(config.serverUrl());
		syncRuneLiteRegistrations();
		log.debug("Partybase party transport: {}", isRelay() ? "relay server" : "RuneLite party");
		if (current != null)
		{
			changeParty(current);
		}
	}

	/** The URL when it is a usable relay address, else null (RuneLite's party). */
	static String validServerUrl(String url)
	{
		String u = url == null ? "" : url.trim();
		if (u.isEmpty())
		{
			return null;
		}
		if (u.startsWith(SECURE_SCHEME) || u.startsWith("ws://localhost") || u.startsWith("ws://127.0.0.1"))
		{
			return u;
		}
		log.warn("Partybase: ignoring server URL {}, it must start with wss://", u);
		return null;
	}

	// ---- RuneLite's party, re-posted as Partybase events ----

	@Subscribe
	public void onPartyChanged(PartyChanged e)
	{
		if (!isRelay())
		{
			wrapped.clear();
			eventBus.post(new PartybasePartyChanged(e.getPassphrase(), e.getPartyId()));
		}
	}

	@Subscribe
	public void onUserJoin(UserJoin e)
	{
		if (!isRelay())
		{
			eventBus.post(new PartybaseUserJoin(e.getPartyId(), e.getMemberId()));
		}
	}

	@Subscribe
	public void onUserPart(UserPart e)
	{
		if (!isRelay())
		{
			wrapped.remove(e.getMemberId());
			eventBus.post(new PartybaseUserPart(e.getMemberId()));
		}
	}

	@Subscribe
	public void onUserSync(UserSync e)
	{
		if (!isRelay())
		{
			eventBus.post(new PartybaseUserSync(e.getMemberId()));
		}
	}

	// priority below PartyDataService (1), so the member is updated before Partybase reads it
	@Subscribe(priority = -1)
	public void onPartyData(PartyData e)
	{
		if (!isRelay())
		{
			eventBus.post(new PartybaseMemberData(e.getMemberId()));
		}
	}

	@Subscribe
	public void onPartyMemberAvatar(PartyMemberAvatar e)
	{
		if (!isRelay())
		{
			eventBus.post(new PartybaseMemberAvatar(e.getMemberId()));
		}
	}

	@Subscribe
	public void onSpecialCounterUpdate(SpecialCounterUpdate e)
	{
		if (!isRelay())
		{
			eventBus.post(new PartybaseSpecialCounter(e));
		}
	}

	/** Keeps the name used when joining a relay party up to date (the websocket thread can't read it). */
	@Subscribe
	public void onGameTick(GameTick e)
	{
		localName = client.getLocalPlayer() == null ? "" : Text.removeTags(client.getLocalPlayer().getName());
		if (isRelay() && joined && socket != null)
		{
			PartyData update = dataSender.tick();
			if (update != null)
			{
				send(update);
			}
		}
	}

	private PartybaseMember wrap(PartyMember m)
	{
		if (m == null)
		{
			return null;
		}
		RuneLiteMember w = wrapped.get(m.getMemberId());
		if (w == null || w.unwrap() != m)
		{
			w = new RuneLiteMember(m);
			wrapped.put(m.getMemberId(), w);
		}
		return w;
	}

	// ---- relay connection ----

	private synchronized void openRelay()
	{
		String url = serverUrl;
		if (url == null || passphrase == null)
		{
			return;
		}
		int gen = ++generation;
		Request.Builder request = new Request.Builder().url(url.replaceFirst("^wss://", "https://").replaceFirst("^ws://", "http://"));
		String key = config.serverKey() == null ? "" : config.serverKey().trim();
		if (!key.isEmpty())
		{
			request.header("Authorization", "Bearer " + key);
		}
		socket = httpClient.newWebSocket(request.build(), new Listener(gen));
	}

	private synchronized void closeRelay()
	{
		generation++;
		joined = false;
		cancel(keepalive);
		cancel(retry);
		keepalive = retry = null;
		WebSocket ws = socket;
		socket = null;
		if (ws != null)
		{
			ws.close(1000, "leaving");
		}
		for (Long id : new ArrayList<>(relayMembers.keySet()))
		{
			if (id != localMemberId)
			{
				eventBus.post(new PartybaseUserPart(id));
			}
		}
		relayMembers.clear();
	}

	private synchronized void scheduleRetry(int gen, String reason)
	{
		if (gen != generation || passphrase == null)
		{
			return;
		}
		joined = false;
		socket = null;
		cancel(keepalive);
		long wait = retrySeconds;
		retrySeconds = (int) Math.min(MAX_RETRY_SECONDS, retrySeconds * 2L);
		log.debug("Partybase relay disconnected ({}), retrying in {} s", reason, wait);
		retry = executor.schedule(() ->
		{
			synchronized (this)
			{
				if (gen == generation)
				{
					openRelay();
				}
			}
		}, wait, TimeUnit.SECONDS);
	}

	private static void cancel(ScheduledFuture<?> f)
	{
		if (f != null)
		{
			f.cancel(false);
		}
	}

	private final class Listener extends WebSocketListener
	{
		private final int gen;

		Listener(int gen)
		{
			this.gen = gen;
		}

		@Override
		public void onOpen(WebSocket ws, Response response)
		{
			if (gen != generation)
			{
				ws.close(1000, "stale");
				return;
			}
			JsonObject join = new JsonObject();
			join.addProperty("action", "join");
			join.addProperty("party", relayParty(passphrase));
			join.addProperty("name", localName);
			join.addProperty("memberId", Long.toString(localMemberId));
			ws.send(gson.toJson(join));
			synchronized (PartyLink.this)
			{
				cancel(keepalive);
				keepalive = executor.scheduleAtFixedRate(() -> ws.send("{\"action\":\"ping\"}"),
					KEEPALIVE_SECONDS, KEEPALIVE_SECONDS, TimeUnit.SECONDS);
			}
		}

		@Override
		public void onMessage(WebSocket ws, String text)
		{
			if (gen != generation)
			{
				return;
			}
			try
			{
				handle(new JsonParser().parse(text).getAsJsonObject());
			}
			catch (RuntimeException ex)
			{
				log.debug("Partybase relay: bad frame {}", text, ex);
			}
		}

		@Override
		public void onClosing(WebSocket ws, int code, String reason)
		{
			ws.close(1000, null);
		}

		@Override
		public void onClosed(WebSocket ws, int code, String reason)
		{
			scheduleRetry(gen, "closed " + code);
		}

		@Override
		public void onFailure(WebSocket ws, Throwable t, Response response)
		{
			int status = response == null ? 0 : response.code();
			if (status == 401 || status == 403)
			{
				// a wrong key won't get better by retrying
				log.warn("Partybase relay refused the connection (HTTP {}): check the server key", status);
				notice("the party server refused your key. Check Party server > Server key.");
				synchronized (PartyLink.this)
				{
					if (gen == generation)
					{
						joined = false;
						socket = null;
						cancel(keepalive);
					}
				}
				return;
			}
			scheduleRetry(gen, t.getMessage());
		}
	}

	private void handle(JsonObject frame)
	{
		String type = string(frame, "type");
		if (type == null)
		{
			return;
		}
		switch (type)
		{
			case "joined":
				onJoined(frame);
				break;
			case "userJoin":
			{
				long id = Long.parseLong(string(frame, "memberId"));
				relayMembers.computeIfAbsent(id, k -> new RelayMember(k, string(frame, "name")));
				eventBus.post(new PartybaseUserJoin(partyId, id));
				break;
			}
			case "userPart":
			{
				long id = Long.parseLong(string(frame, "memberId"));
				if (relayMembers.remove(id) != null)
				{
					eventBus.post(new PartybaseUserPart(id));
				}
				break;
			}
			case "message":
				onRelayMessage(Long.parseLong(string(frame, "from")), frame.getAsJsonObject("data"));
				break;
			case "error":
				log.debug("Partybase relay error: {}", string(frame, "error"));
				break;
			default:
				break;
		}
	}

	private void onJoined(JsonObject frame)
	{
		long id = Long.parseLong(string(frame, "memberId"));
		Map<Long, String> listed = new ConcurrentHashMap<>();
		for (JsonElement m : frame.getAsJsonArray("members"))
		{
			JsonObject o = m.getAsJsonObject();
			listed.put(Long.parseLong(string(o, "memberId")), string(o, "name") == null ? "" : string(o, "name"));
		}
		// reconcile with what we knew before a reconnect
		for (Long known : new ArrayList<>(relayMembers.keySet()))
		{
			if (!listed.containsKey(known))
			{
				relayMembers.remove(known);
				eventBus.post(new PartybaseUserPart(known));
			}
		}
		localMemberId = id;
		joined = true;
		retrySeconds = 2;
		for (Map.Entry<Long, String> e : listed.entrySet())
		{
			if (relayMembers.putIfAbsent(e.getKey(), new RelayMember(e.getKey(), e.getValue())) == null)
			{
				eventBus.post(new PartybaseUserJoin(partyId, e.getKey()));
			}
		}
		// ask everyone (and our own data sender) for their full state, as RuneLite's party does after a join
		dataSender.forceNext();
		UserSync sync = new UserSync();
		send(sync);
	}

	private void onRelayMessage(long from, JsonObject envelope)
	{
		RelayMember sender = relayMembers.get(from);
		String type = envelope == null ? null : string(envelope, "t");
		Class<? extends PartyMemberMessage> cls = type == null ? null : types.get(type);
		if (sender == null || cls == null)
		{
			return;
		}
		PartyMemberMessage message = gson.fromJson(envelope.get("d"), cls);
		message.setMemberId(from);
		switch (type)
		{
			case SYNC_TYPE:
				if (from != localMemberId)
				{
					dataSender.forceNext();
				}
				eventBus.post(new PartybaseUserSync(from));
				break;
			case DATA_TYPE:
				sender.apply((PartyData) message);
				eventBus.post(new PartybaseMemberData(from));
				break;
			case CHAT_TYPE:
				showChat(sender, ((PartyChatMessage) message).getValue());
				break;
			case SPEC_TYPE:
				eventBus.post(new PartybaseSpecialCounter((SpecialCounterUpdate) message));
				break;
			default:
				eventBus.post(message);
				break;
		}
	}

	/** Party chat from the relay, shown like RuneLite's party chat. */
	private void showChat(RelayMember sender, String value)
	{
		if (value == null || !sender.isLoggedIn())
		{
			return;
		}
		String text = Text.removeTags(value).replaceAll("<img=.+>", "");
		if (text.length() > MAX_CHAT_LENGTH)
		{
			text = text.substring(0, MAX_CHAT_LENGTH);
		}
		chat.queue(QueuedMessage.builder()
			.type(ChatMessageType.FRIENDSCHAT)
			.sender("Party")
			.name(sender.getDisplayName())
			.runeLiteFormattedMessage(text)
			.build());
	}

	private void notice(String message)
	{
		chat.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage("Partybase: " + message)
			.build());
	}

	/** The relay accepts letters, digits and hyphens; RuneLite passphrases may have spaces. */
	static String relayParty(String passphrase)
	{
		String p = passphrase == null ? "" : passphrase.trim().replaceAll("[^A-Za-z0-9-]+", "-");
		return p.length() > 64 ? p.substring(0, 64) : p;
	}

	/** The same id RuneLite derives from a passphrase, so party ids match on either transport. */
	static long passphraseToId(String passphrase)
	{
		return Hashing.sha256().hashBytes(passphrase.getBytes(StandardCharsets.UTF_8)).asLong() & Long.MAX_VALUE;
	}

	/** A positive 53-bit id: fits JSON numbers on the server side. */
	private long randomMemberId()
	{
		return 1 + (random.nextLong() & ((1L << 53) - 2));
	}

	private static String string(JsonObject o, String name)
	{
		JsonElement e = o.get(name);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}
}
