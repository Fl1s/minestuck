package com.mraof.minestuck.client.godtier;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mraof.minestuck.player.EnumClass;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.*;

public final class GodTierArmorModel extends HumanoidModel<LivingEntity>
{
	private record Behavior(Map<String, String> follow, Map<String, EquipmentSlot> slots, Set<String> hideable,
	                        EquipmentSlot skirtSlot)
	{
	}
	
	private static final EquipmentSlot HEAD = EquipmentSlot.HEAD, CHEST = EquipmentSlot.CHEST, LEGS = EquipmentSlot.LEGS, FEET = EquipmentSlot.FEET;
	private static final Behavior DEFAULT = new Behavior(Map.of(), Map.of(), Set.of(), LEGS);
	private static final Map<EnumClass, Behavior> BEHAVIORS = new EnumMap<>(EnumClass.class);
	
	static
	{
		for(EnumClass c : List.of(EnumClass.MAID, EnumClass.MAGE, EnumClass.SEER, EnumClass.THIEF)) // skirt sits on the shirt piece
			BEHAVIORS.put(c, new Behavior(Map.of(), Map.of(), Set.of(), CHEST));
		BEHAVIORS.put(EnumClass.ROGUE, new Behavior(Map.of(), Map.of(), Set.of("mask"), CHEST));
		BEHAVIORS.put(EnumClass.MUSE, new Behavior(Map.of("neckLeft", "leftArm", "neckRight", "rightArm"), Map.of("neckLeft", HEAD, "neckRight", HEAD), Set.of(), CHEST));
		BEHAVIORS.put(EnumClass.LORD, new Behavior(Map.of("suspenders", "torso", "coat", "torso", "leftSleeve", "leftArm", "rightSleeve", "rightArm", "coatLeft", "leftLeg", "coatRight", "rightLeg"), Map.of("coat", CHEST, "suspenders", LEGS, "coatLeft", CHEST, "coatRight", CHEST, "leftSleeve", CHEST, "rightSleeve", CHEST), Set.of("coat", "suspenders", "coatLeft", "coatRight", "leftSleeve", "rightSleeve"), LEGS));
		BEHAVIORS.put(EnumClass.SYLPH, new Behavior(Map.of("hoodThingsParent", "skirtFront", "skirtFrontShirt", "skirtFront", "skirtMiddleShirt", "skirtMiddle", "skirtBackShirt", "skirtBack"), Map.of(), Set.of(), LEGS)); // Sylph visibility is dynamic, see applySylph
	}
	
	private static final Map<EnumClass, GodTierArmorModel> CACHE = new EnumMap<>(EnumClass.class);
	
	@Nullable
	public static GodTierArmorModel get(EnumClass heroClass)
	{
		if(CACHE.containsKey(heroClass)) return CACHE.get(heroClass);
		GodTierModelSpec spec = GodTierModelSpec.get(heroClass);
		GodTierArmorModel model = spec == null ? null : build(spec);
		CACHE.put(heroClass, model);
		return model;
	}
	
	public static void clearCache()
	{
		CACHE.clear();
	}
	
	private final EnumClass heroClass;
	private final Behavior behavior;
	private final ModelPart godRoot;
	private final Map<String, ModelPart> parts;
	private final Map<String, float[]> restPivot = new HashMap<>();
	
	private GodTierArmorModel(GodTierModelSpec spec, ModelPart godRoot, Map<String, ModelPart> parts)
	{
		super(dummyRoot());
		this.heroClass = spec.heroClass();
		this.behavior = BEHAVIORS.getOrDefault(heroClass, DEFAULT);
		this.godRoot = godRoot;
		this.parts = parts;
		for(GodTierModelSpec.Part p : spec.parts())
			restPivot.put(p.name(), p.pivot());
	}
	
	private static ModelPart dummyRoot()
	{
		MeshDefinition mesh = new MeshDefinition();
		for(String name : List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg"))
			mesh.getRoot().addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
		return LayerDefinition.create(mesh, 64, 32).bakeRoot();
	}
	
	private static GodTierArmorModel build(GodTierModelSpec spec)
	{
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> defs = new HashMap<>();
		Map<String, String> parentOf = new HashMap<>();
		
		List<GodTierModelSpec.Part> pending = new ArrayList<>(spec.parts());
		int guard = pending.size() + 1;
		while(!pending.isEmpty() && guard-- > 0)
		{
			for(Iterator<GodTierModelSpec.Part> it = pending.iterator(); it.hasNext(); )
			{
				GodTierModelSpec.Part p = it.next();
				if(p.parent() != null && !defs.containsKey(p.parent())) continue;
				CubeListBuilder cubes = CubeListBuilder.create();
				for(GodTierModelSpec.Cube c : p.cubes())
					cubes.texOffs(c.u(), c.v()).mirror(c.mirror()).addBox(c.x(), c.y(), c.z(), c.w(), c.h(), c.d(), new CubeDeformation(c.inflate()));
				PartDefinition parent = p.parent() == null ? mesh.getRoot() : defs.get(p.parent());
				defs.put(p.name(), parent.addOrReplaceChild(p.name(), cubes, PartPose.offsetAndRotation(p.pivot()[0], p.pivot()[1], p.pivot()[2], p.rot()[0], p.rot()[1], p.rot()[2])));
				parentOf.put(p.name(), p.parent());
				it.remove();
			}
		}
		
		mesh.getRoot().addOrReplaceChild("symbol", CubeListBuilder.create().texOffs(spec.symbolUv()[0], spec.symbolUv()[1]).addBox(-3.0F, 2.0F, -2.0F, 6.0F, 6.0F, 0.0F, new CubeDeformation(0.2512F)), PartPose.ZERO);
		parentOf.put("symbol", null);
		
		ModelPart root = LayerDefinition.create(mesh, spec.texW(), spec.texH()).bakeRoot();
		Map<String, ModelPart> parts = new HashMap<>();
		for(String name : parentOf.keySet())
		{
			Deque<String> chain = new ArrayDeque<>();
			for(String n = name; n != null; n = parentOf.get(n))
				chain.push(n);
			ModelPart part = root;
			for(String n : chain)
				part = part.getChild(n);
			parts.put(name, part);
		}
		return new GodTierArmorModel(spec, root, parts);
	}
	
	public void setup(LivingEntity entity, HumanoidModel<?> original, EquipmentSlot slot, boolean hideExtras, @Nullable EnumClass legsClass)
	{
		ModelPart head = p("head"), hood = p("hood"), headsock = p("headsock"), neck = p("neck"), torso = p("torso"), cape = p("cape"), leftArm = p("leftArm"), rightArm = p("rightArm"), skirtFront = p("skirtFront"), skirtMiddle = p("skirtMiddle"), skirtBack = p("skirtBack"), belt = p("belt"), leftLeg = p("leftLeg"), rightLeg = p("rightLeg"), leftFoot = p("leftFoot"), rightFoot = p("rightFoot"), symbol = p("symbol");
		
		head.visible = hood.visible = neck.visible = slot == HEAD;
		symbol.visible = torso.visible = leftArm.visible = rightArm.visible = slot == CHEST;
		cape.visible = slot == HEAD;
		skirtFront.visible = skirtMiddle.visible = skirtBack.visible = slot == behavior.skirtSlot();
		belt.visible = leftLeg.visible = rightLeg.visible = slot == LEGS;
		leftFoot.visible = rightFoot.visible = slot == FEET;
		behavior.slots().forEach((name, s) -> p(name).visible = slot == s);
		for(String name : behavior.hideable())
		{
			ModelPart part = p(name);
			part.visible = part.visible && !hideExtras;
		}
		if(heroClass == EnumClass.ROGUE) p("mask").visible = !hideExtras;
		if(heroClass == EnumClass.SYLPH) applySylph(slot, legsClass);
		
		head.copyFrom(original.head);
		hood.copyFrom(original.head);
		neck.copyFrom(original.body);
		torso.copyFrom(original.body);
		symbol.copyFrom(original.body);
		cape.copyFrom(original.body);
		belt.copyFrom(original.body);
		leftArm.copyFrom(original.leftArm);
		rightArm.copyFrom(original.rightArm);
		leftLeg.copyFrom(original.leftLeg);
		rightLeg.copyFrom(original.rightLeg);
		leftFoot.copyFrom(original.leftLeg);
		rightFoot.copyFrom(original.rightLeg);
		
		double dx = entity.getX() - entity.xo, dy = Math.max(0, entity.getY() - entity.yo), dz = entity.getZ() - entity.zo;
		cape.xRot += (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * entity.walkAnimation.speed();
		
		resetPivot("skirtFront");
		resetPivot("skirtMiddle");
		resetPivot("skirtBack");
		if(original.crouching) for(ModelPart skirt : List.of(skirtFront, skirtMiddle, skirtBack))
		{
			skirt.z += 4;
			skirt.y -= 2;
		}
		skirtBack.xRot = Math.max(0, Math.max(leftLeg.xRot, rightLeg.xRot));
		skirtFront.xRot = Math.min(0, Math.min(leftLeg.xRot, rightLeg.xRot));
		hood.xRot = Math.max(0, head.xRot);
		hood.y = (float) (-Math.min(0, Math.sin(head.xRot))) * 8f;
		headsock.z = (float) -Math.min(0, head.xRot);
		
		behavior.follow().forEach((extra, source) -> p(extra).copyFrom(p(source)));
	}
	
	private void applySylph(EquipmentSlot slot, @Nullable EnumClass legsClass)
	{
		boolean sylphDress = legsClass == EnumClass.SYLPH;
		boolean witchDress = legsClass == EnumClass.WITCH;
		p("leftLeg").visible = p("rightLeg").visible = slot == CHEST && !sylphDress; // sic: matches 1.12.2
		p("hoodThingsParent").visible = slot == HEAD && (sylphDress || witchDress);
		p("hoodSkirtlessLeft").visible = p("hoodSkirtlessRight").visible = slot == HEAD && !(sylphDress || witchDress);
		p("skirtFrontShirt").visible = p("skirtMiddleShirt").visible = p("skirtBackShirt").visible = slot == CHEST && sylphDress;
		p("hoodThings").xRot = sylphDress ? -0.1745F : -0.2618F;
	}
	
	private void resetPivot(String name)
	{
		ModelPart part = p(name);
		float[] rest = restPivot.get(name);
		part.x = rest[0];
		part.y = rest[1];
		part.z = rest[2];
	}
	
	private ModelPart p(String name)
	{
		return parts.get(name);
	}
	
	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color)
	{
		godRoot.render(poseStack, buffer, packedLight, packedOverlay, color);
	}
}