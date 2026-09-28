package com.mraof.minestuck.client.godtier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mraof.minestuck.Minestuck;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record GodTierModelSpec(EnumClass heroClass, int texW, int texH, int[] symbolUv, List<Part> parts)
{
	public record Cube(int u, int v, float x, float y, float z, int w, int h, int d, float inflate, boolean mirror)
	{
	}
	
	public record Part(String name, @Nullable String parent, float[] pivot, float[] rot, List<Cube> cubes)
	{
	}
	
	private static final Map<EnumClass, GodTierModelSpec> CACHE = new EnumMap<>(EnumClass.class);
	
	@Nullable
	public static GodTierModelSpec get(EnumClass heroClass)
	{
		if(CACHE.containsKey(heroClass)) return CACHE.get(heroClass);
		GodTierModelSpec spec = load(heroClass);
		CACHE.put(heroClass, spec);
		return spec;
	}
	
	public static void clearCache()
	{
		CACHE.clear();
	}
	
	@Nullable
	private static GodTierModelSpec load(EnumClass heroClass)
	{
		ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Minestuck.MOD_ID, "godtier_models/" + heroClass.name().toLowerCase(Locale.ROOT) + ".json");
		try(Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(id))
		{
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			JsonArray uv = json.getAsJsonArray("symbolUv");
			List<Part> parts = new ArrayList<>();
			for(JsonElement pe : json.getAsJsonArray("parts"))
			{
				JsonObject p = pe.getAsJsonObject();
				List<Cube> cubes = new ArrayList<>();
				for(JsonElement ce : p.getAsJsonArray("cubes"))
				{
					JsonArray c = ce.getAsJsonArray();
					cubes.add(new Cube(c.get(0).getAsInt(), c.get(1).getAsInt(), c.get(2).getAsFloat(), c.get(3).getAsFloat(), c.get(4).getAsFloat(), c.get(5).getAsInt(), c.get(6).getAsInt(), c.get(7).getAsInt(), c.get(8).getAsFloat(), c.get(9).getAsBoolean()));
				}
				parts.add(new Part(p.get("name").getAsString(), p.get("parent").isJsonNull() ? null : p.get("parent").getAsString(), floats(p.getAsJsonArray("pivot")), floats(p.getAsJsonArray("rot")), cubes));
			}
			return new GodTierModelSpec(heroClass, json.get("texW").getAsInt(), json.get("texH").getAsInt(), new int[]{uv.get(0).getAsInt(), uv.get(1).getAsInt()}, parts);
		} catch(IOException | RuntimeException e)
		{
			org.apache.logging.log4j.LogManager.getLogger().error("Could not load god tier model {}", id, e);
			return null;
		}
	}
	
	private static float[] floats(JsonArray a)
	{
		return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
	}
}