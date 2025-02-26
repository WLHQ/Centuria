package org.asf.centuria.packets.xt.gameserver.sanctuary;

import java.io.IOException;

import org.asf.centuria.Centuria;
import org.asf.centuria.data.XtReader;
import org.asf.centuria.data.XtWriter;
import org.asf.centuria.entities.players.Player;
import org.asf.centuria.networking.smartfox.SmartfoxClient;
import org.asf.centuria.packets.xt.IXtPacket;
import org.asf.centuria.packets.xt.gameserver.inventory.InventoryItemPacket;

public class SanctuaryLookSavePacket implements IXtPacket<SanctuaryLookSavePacket> {

	private static final String PACKET_ID = "sls";

	public String lookSlotId = null;
	public String lookSlotName = null;

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public SanctuaryLookSavePacket instantiate() {
		return new SanctuaryLookSavePacket();
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		lookSlotId = reader.read();
		lookSlotName = reader.read();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
		writer.writeInt(DATA_PREFIX); // Data prefix

		writer.writeString(lookSlotId);

		writer.writeString(DATA_SUFFIX); // Data suffix
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Switch sanctuary look
		Player plr = (Player) client.container;

		// Log
		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Client to server (lookSlotId: " + lookSlotId
					+ ", lookSlotName: " + lookSlotName + ")");
		}

		// save active look into that slot

		plr.account.getSaveSpecificInventory().getSanctuaryAccessor().saveSanctuaryLookToSlot(plr.activeSanctuaryLook,
				lookSlotId, lookSlotName);

		// send an il response

		var il = plr.account.getSaveSpecificInventory().getItem("201");
		var ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		plr.client.sendPacket(ilPacket);

		// send this packet

		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Server to client IL: " + ilPacket.build());
		}

		il = plr.account.getSaveSpecificInventory().getItem("5");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		plr.client.sendPacket(ilPacket);

		// send this packet

		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Server to client IL: " + ilPacket.build());
		}

		il = plr.account.getSaveSpecificInventory().getItem("6");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		plr.client.sendPacket(ilPacket);

		// send this packet

		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Server to client IL: " + ilPacket.build());
		}

		il = plr.account.getSaveSpecificInventory().getItem("10");
		ilPacket = new InventoryItemPacket();
		ilPacket.item = il;

		// send IL
		plr.client.sendPacket(ilPacket);

		// send this packet

		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Server to client IL: " + ilPacket.build());
		}

		plr.client.sendPacket(this);

		if (Centuria.debugMode) {
			System.out.println("[SANCTUARYEDITOR] [SAVELOOK]  Server to client SSL: " + this.build());
		}

		return true;
	}

}
