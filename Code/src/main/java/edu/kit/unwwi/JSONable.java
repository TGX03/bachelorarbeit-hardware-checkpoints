package edu.kit.unwwi;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public interface JSONable {

	@NotNull
	JSONObject toJSON();
}
