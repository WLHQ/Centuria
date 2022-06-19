package org.asf.emuferal.packets.xt;

import java.io.IOException;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;

public interface IXtPacket<T extends IXtPacket<T>> extends ISmartfoxPacket {

	public default boolean canParse(String content) {
		if (!content.startsWith("%xt%"))
			return false;
		
		XtReader rd = new XtReader(content);
		String packetID = rd.read();
		if (!packetID.equals(id()))
			return false;
		return true;
	}

	public default boolean parse(String content) throws IOException {
		if (!content.startsWith("%xt%"))
			return false;
		
		//if (System.getProperty("debugMode") != null) {
			//System.out.println("client to server: " + content);
		//}
				
		
		XtReader rd = new XtReader(content);
		String packetID = rd.read();
		if (!packetID.equals(id()))
			return false;
		parse(rd);
		return true;
	}

	public default String build() throws IOException {
		XtWriter writer = new XtWriter();
		writer.writeString(id());
		build(writer);
		return writer.encode();
	}

	/**
	 * Creates a new instance of this packet type
	 * 
	 * @return New packet instance
	 */
	public T instantiate();

	/**
	 * Defines the packet ID
	 * 
	 * @return Packet ID string
	 */
	public abstract String id();

	/**
	 * Reads the packet content
	 * 
	 * @param reader Packet reader
	 */
	public void parse(XtReader reader) throws IOException;

	/**
	 * Writes the packet content to the output writer
	 * 
	 * @param writer Packet writer
	 */
	public void build(XtWriter writer) throws IOException;

}
