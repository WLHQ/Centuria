package org.asf.centuria.networking.http.api.custom;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.asf.centuria.Centuria;
import org.asf.centuria.accounts.AccountManager;
import org.asf.centuria.accounts.CenturiaAccount;
import org.asf.centuria.accounts.SaveMode;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SaveManagerHandler extends HttpPushProcessor {

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		try {
			if (!method.equalsIgnoreCase("post")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Parse body
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			getRequest().transferRequestBody(strm);
			byte[] body = strm.toByteArray();
			strm.close();

			// Parse body
			JsonObject request = JsonParser.parseString(new String(body, "UTF-8")).getAsJsonObject();
			if (!request.has("command")) {
				this.setResponseStatus(400, "Bad request");
				return;
			}
			String command = request.get("command").getAsString();

			// Load manager
			AccountManager manager = AccountManager.getInstance();

			// Parse JWT payload
			String token = this.getHeader("Authorization").substring("Bearer ".length());
			if (token.isBlank()) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			// Parse token
			if (token.isBlank()) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			// Verify signature
			String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
			String sig = token.split("\\.")[2];
			if (!Centuria.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			// Verify expiry
			JsonObject jwtPl = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();
			if (!jwtPl.has("exp") || jwtPl.get("exp").getAsLong() < System.currentTimeMillis() / 1000) {
				this.setResponseStatus(403, "Access denied");
				return;
			}

			JsonObject payload = JsonParser
					.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
					.getAsJsonObject();

			// Find account
			String id = payload.get("uuid").getAsString();

			// Check existence
			if (id == null) {
				// Invalid details
				this.setResponseContent("text/json", "{\"error\":\"invalid_credential\"}");
				this.setResponseStatus(401, "Unauthorized");
				return;
			}

			// Find account
			CenturiaAccount acc = manager.getAccount(id);
			if (acc == null) {
				this.setResponseStatus(401, "Unauthorized");
				return;
			}

			// Prepare response
			JsonObject response = new JsonObject();

			// Check mode
			if (acc.getSaveMode() == SaveMode.SINGLE) {
				// Check command, we can only migrate
				if (command.equals("migrate")) {
					// Migrate
					acc.migrateSaveDataToManagedMode();
					response.addProperty("status", "success");
					response.addProperty("migrated", true);
					setResponseContent("text/json", response.toString());
					return;
				}
				response.addProperty("status", "failure");
				response.addProperty("error", "save_data_unmanaged");
				response.addProperty("error_message",
						"Running in unmanaged save data mode, please migrate to managed mode before using the save manager.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				return;
			}

			// Handle command
			switch (command) {
			case "setactive": {
				if (!request.has("save")) {
					// Missing parameter
					response.addProperty("status", "failure");
					response.addProperty("error", "missing_argument");
					response.addProperty("error_message", "Missing required argument.");
					setResponseContent("text/json", response.toString());
					this.setResponseStatus(400, "Bad request");
					break;
				}
				if (!acc.getSaveManager().switchSave(request.get("save").getAsString())) {
					// Failure
					response.addProperty("status", "failure");
					response.addProperty("error", "invalid_save");
					response.addProperty("error_message", "Invalid save name.");
					setResponseContent("text/json", response.toString());
					this.setResponseStatus(400, "Bad request");
					break;
				}
				response.addProperty("status", "success");
				response.addProperty("active", acc.getSaveManager().getCurrentActiveSave());
				acc.kickDirect("SYSTEM", "Save data switched");
				break;
			}
			case "getactive": {
				response.addProperty("status", "success");
				response.addProperty("active", acc.getSaveManager().getCurrentActiveSave());
				break;
			}
			case "list": {
				response.addProperty("status", "success");
				JsonArray arr = new JsonArray();
				for (String save : acc.getSaveManager().getSaves())
					arr.add(save);
				response.add("saves", arr);
				break;
			}
			default: {
				// Unknown
				response.addProperty("status", "failure");
				response.addProperty("error", "unknown_command");
				response.addProperty("error_message", "Unknown command.");
				setResponseContent("text/json", response.toString());
				this.setResponseStatus(400, "Bad request");
				break;
			}
			}

			setResponseContent("text/json", response.toString());
		} catch (Exception e) {
			setResponseStatus(500, "Internal Server Error");
			Centuria.logger.error(getRequest().getRequestPath() + " failed: 500: Internal Server Error", e);
		}
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new SaveManagerHandler();
	}

	@Override
	public String path() {
		return "/centuria/savemanager";
	}

}
