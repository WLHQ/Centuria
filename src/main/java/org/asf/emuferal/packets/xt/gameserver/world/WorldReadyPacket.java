package org.asf.emuferal.packets.xt.gameserver.world;

import java.io.IOException;
import java.io.InputStream;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.gameserver.GameServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.packets.xt.gameserver.inventory.InventoryItemDownloadPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WorldReadyPacket implements IXtPacket<WorldReadyPacket> {

	public String teleportUUID = "";

	@Override
	public WorldReadyPacket instantiate() {
		return new WorldReadyPacket();
	}

	@Override
	public String id() {
		return "wr";
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		teleportUUID = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Find the world coordinates

		// Load player
		Player plr = (Player) client.container;

		// Sync
		GameServer srv = (GameServer) client.getServer();
		for (Player player : srv.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.destroyAt(player);
			}
		}

		// Disable sync
		plr.room = null;

		// Send to tutorial if new
		if (plr.account.isPlayerNew()) {
			// Tutorial spawn
			WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
			res.x = 107.67;
			res.y = 8.85;
			res.z = -44.85;
			res.rx = 0;
			res.ry = 0.9171;
			res.rz = -0;
			res.rw = 0.3987;
			client.sendPacket(res);
			return true;
		}

		try {
			Thread.sleep(5000); // Temporary wait
		} catch (InterruptedException e) {
		}

		// Find spawn
		handleSpawn(teleportUUID, plr, client);

		// Set location
		plr.lastLocation = plr.respawn;

		// Send all other players to the current player
		GameServer server = (GameServer) client.getServer();
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				player.syncTo(plr);
			}
		}

		// Sync spawn
		for (Player player : server.getPlayers()) {
			if (plr.room != null && player.room != null && player.room.equals(plr.room) && player != plr) {
				plr.syncTo(player);
			}
		}

		return true;
	}

	private void handleSpawn(String room, Player plr, SmartfoxClient client) throws IOException {
		// Find teleport
		// Load spawn helper
		try {
			// Load helper
			InputStream strm = InventoryItemDownloadPacket.class.getClassLoader().getResourceAsStream("spawns.json");
			JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
					.get("Spawns").getAsJsonObject();
			strm.close();

			// Check existence
			if (helper.has(room)) {
				// Send response
				helper = helper.get(room).getAsJsonObject();
				plr.room = helper.get("worldID").getAsString();
				System.out.println("Player teleport: " + plr.account.getDisplayName() + ": " + plr.room);
				WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
				res.x = helper.get("spawnX").getAsDouble();
				res.y = helper.get("spawnY").getAsDouble();
				res.z = helper.get("spawnZ").getAsDouble();
				res.rw = helper.get("spawnRotW").getAsDouble();
				res.rx = helper.get("spawnRotX").getAsDouble();
				res.ry = helper.get("spawnRotY").getAsDouble();
				res.rz = helper.get("spawnRotZ").getAsDouble();
				client.sendPacket(res);
				plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%"
						+ res.rz;
				return;
			}
		} catch (IOException e) {
		}

		// Spawn not found
		System.err.println("Player teleport: " + plr.account.getDisplayName() + " to unrecognized spawn!");
		WorldObjectInfoAvatarLocal res = new WorldObjectInfoAvatarLocal();
		res.x = 0;
		res.y = 80;
		res.z = 0;
		res.rw = 0;
		res.rx = 0;
		res.ry = 0;
		res.rz = 0;
		client.sendPacket(res);
		plr.respawn = res.x + "%" + res.y + "%" + res.z + "%" + res.rw + "%" + res.rx + "%" + res.ry + "%" + res.rz;
	}

}
