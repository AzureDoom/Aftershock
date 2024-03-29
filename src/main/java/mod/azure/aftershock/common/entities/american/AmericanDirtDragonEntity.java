package mod.azure.aftershock.common.entities.american;

import java.util.List;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.aftershock.common.AftershockMod;
import mod.azure.aftershock.common.AftershockMod.ModMobs;
import mod.azure.aftershock.common.AftershockMod.ModSounds;
import mod.azure.aftershock.common.entities.base.AfterShockVibrationUser;
import mod.azure.aftershock.common.entities.base.BaseEntity;
import mod.azure.aftershock.common.entities.base.SoundTrackingEntity;
import mod.azure.aftershock.common.entities.tasks.GraboidAttackTask;
import mod.azure.aftershock.common.entities.tasks.SoundPanic;
import mod.azure.aftershock.common.helpers.AftershockAnimationsDefault;
import mod.azure.aftershock.common.helpers.AttackType;
import mod.azure.azurelib.ai.pathing.AzureNavigation;
import mod.azure.azurelib.core.animation.AnimatableManager.ControllerRegistrar;
import mod.azure.azurelib.core.animation.Animation.LoopType;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.RawAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.tslat.smartbrainlib.api.SmartBrainOwner;
import net.tslat.smartbrainlib.api.core.BrainActivityGroup;
import net.tslat.smartbrainlib.api.core.SmartBrainProvider;
import net.tslat.smartbrainlib.api.core.behaviour.FirstApplicableBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.OneRandomBehaviour;
import net.tslat.smartbrainlib.api.core.behaviour.custom.look.LookAtTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.misc.Idle;
import net.tslat.smartbrainlib.api.core.behaviour.custom.move.MoveToWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetRandomWalkTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.path.SetWalkTargetToAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.InvalidateAttackTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.SetRandomLookTarget;
import net.tslat.smartbrainlib.api.core.behaviour.custom.target.TargetOrRetaliate;
import net.tslat.smartbrainlib.api.core.sensor.ExtendedSensor;
import net.tslat.smartbrainlib.api.core.sensor.custom.UnreachableTargetSensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.HurtBySensor;
import net.tslat.smartbrainlib.api.core.sensor.vanilla.NearbyLivingEntitySensor;

public class AmericanDirtDragonEntity extends SoundTrackingEntity implements SmartBrainOwner<AmericanDirtDragonEntity> {

	public AmericanDirtDragonEntity(EntityType<? extends BaseEntity> entityType, Level level) {
		super(entityType, level);
		// Sets the speed and range of vibrations
		this.vibrationUser = new AfterShockVibrationUser(this, 1.25F, 15);
		// Sets exp drop amount
		this.xpReward = AftershockMod.config.americandirtdevil_exp;
	}

	// Animation logic
	@Override
	public void registerControllers(ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "livingController", 5, event -> {
			var isDead = this.dead || this.getHealth() < 0.01 || this.isDeadOrDying();
			if (event.isMoving() && this.getLastDamageSource() == null)
				return event.setAndContinue(AftershockAnimationsDefault.WALK);
			return event.setAndContinue(this.getLastDamageSource() != null && this.hurtDuration > 0 && !isDead ? AftershockAnimationsDefault.HURT : AftershockAnimationsDefault.IDLE);
		}).setSoundKeyframeHandler(event -> {
			if (event.getKeyframeData().getSound().matches("attacking"))
				if (this.level().isClientSide)
					this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.DIRTDRAG0N_ATTACK, SoundSource.HOSTILE, 1.25F, 1.0F, true);
			if (event.getKeyframeData().getSound().matches("dying"))
				if (this.level().isClientSide)
					this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModSounds.SHREIKER_HURT, SoundSource.HOSTILE, 1.5F, 0.3F, true);
		}).triggerableAnim("death", AftershockAnimationsDefault.DEATH)
				.triggerableAnim("attack", RawAnimation.begin().then("attack", LoopType.PLAY_ONCE)));
	}

	// Brain logic
	@Override
	protected Brain.Provider<?> brainProvider() {
		return new SmartBrainProvider<>(this);
	}

	@Override
	public List<ExtendedSensor<AmericanDirtDragonEntity>> getSensors() {
		return ObjectArrayList.of(
				// Checks living targets it can see is a heat giving entity via the tag or entities on fire.
				new NearbyLivingEntitySensor<AmericanDirtDragonEntity>().setPredicate((target, entity) -> target.isAlive() && entity.hasLineOfSight(target) && (!(target instanceof BaseEntity || (target.getMobType() == MobType.UNDEAD && !target.isOnFire()) || target instanceof EnderMan || target instanceof Endermite || target instanceof Creeper || target instanceof AbstractGolem) || target.getType().is(AftershockMod.HEAT_ENTITY) || target.isOnFire())),
				// Checks for what last hurt it
				new HurtBySensor<>(),
				// Checks if target is unreachable
				new UnreachableTargetSensor<AmericanDirtDragonEntity>());
	}

	@Override
	public BrainActivityGroup<AmericanDirtDragonEntity> getCoreTasks() {
		return BrainActivityGroup.coreTasks(
				// Run from sounds
				new SoundPanic(1.5F),
				// Looks at Target
				new LookAtTarget<>(), new LookAtTargetSink(40, 300),
				// Walks or runs to Target
				new MoveToWalkTarget<>());
	}

	@Override
	public BrainActivityGroup<AmericanDirtDragonEntity> getIdleTasks() {
		return BrainActivityGroup.idleTasks(new FirstApplicableBehaviour<AmericanDirtDragonEntity>(
				// Target or attack/ alerts other entities of this type in range of target.
				new TargetOrRetaliate<>(),
				// Chooses random look target
				new SetRandomLookTarget<>()),
				new OneRandomBehaviour<>(
						// Radius it will walk around in
						new SetRandomWalkTarget<>().setRadius(20).speedModifier(1.1f),
						// Idles the mob so it doesn't do anything
						new Idle<>().runFor(entity -> entity.getRandom().nextInt(300, 600))));
	}

	@Override
	public BrainActivityGroup<AmericanDirtDragonEntity> getFightTasks() {
		return BrainActivityGroup.fightTasks(
				// Removes entity from being a target.
				new InvalidateAttackTarget<>().invalidateIf((entity, target) -> !target.isAlive() || target instanceof Player && ((Player) target).isCreative()),
				// Moves to traget to attack
				new SetWalkTargetToAttackTarget<>().speedMod(1.5F)
				// Attacks the target if in range and is grown enough
				, new GraboidAttackTask<>(10));
	}

	@Override
	protected void customServerAiStep() {
		// Tick the brain
		tickBrain(this);
		super.customServerAiStep();
	}

	// Mob stats
	public static AttributeSupplier.Builder createMobAttributes() {
		return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 25.0D).add(Attributes.MAX_HEALTH, AftershockMod.config.americandirtdevil_health).add(Attributes.ATTACK_DAMAGE, AftershockMod.config.americandirtdevil_damage).add(Attributes.MOVEMENT_SPEED, 0.25D).add(Attributes.ATTACK_KNOCKBACK, 0.0D);
	}

	// Mob Navigation
	@Override
	protected PathNavigation createNavigation(Level world) {
		return new AzureNavigation(this, world);
	}

	// Growth logic
	@Override
	public float getMaxGrowth() {
		return 168000;
	}

	@Override
	public LivingEntity growInto() {
		// grow into American Graboid
		var entity = ModMobs.AMERICAN_GRABOID.create(level());
		if (hasCustomName())
			entity.setCustomName(this.getCustomName());
		if (isAlbino())
			entity.setAlbinoStatus(true);
		entity.setNewBornStatus(true);
		entity.setGrowth(0);
		entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 100, false, false));
		var areaEffectCloudEntity = new AreaEffectCloud(this.level(), this.getX(), this.getY() + 1, this.getZ());
		areaEffectCloudEntity.setRadius(1.0F);
		areaEffectCloudEntity.setDuration(20);
		areaEffectCloudEntity.setParticle(ParticleTypes.POOF);
		areaEffectCloudEntity.setRadiusPerTick(-areaEffectCloudEntity.getRadius() / (float) areaEffectCloudEntity.getDuration());
		entity.level().addFreshEntity(areaEffectCloudEntity);
		return entity;
	}

	/**
	 * Prevents entity collisions from moving the egg.
	 */
	@Override
	public void doPush(Entity entity) {
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	/**
	 * Prevents the egg from being pushed.
	 */
	@Override
	public boolean isPushable() {
		return false;
	}

	/**
	 * Prevents fluids from moving the egg.
	 */
	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	// Checks if should be removed when far way.
	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	// Checks if it should spawn as an adult
	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, SpawnGroupData entityData, CompoundTag entityNbt) {
		// Spawn grown if used with summon command or egg.
		if (spawnReason == MobSpawnType.COMMAND || spawnReason == MobSpawnType.SPAWN_EGG)
			setGrowth(1250);
		if (spawnReason == MobSpawnType.COMMAND || spawnReason == MobSpawnType.SPAWN_EGG || spawnReason == MobSpawnType.SPAWNER || spawnReason == MobSpawnType.NATURAL || spawnReason == MobSpawnType.BREEDING || spawnReason == MobSpawnType.MOB_SUMMONED || spawnReason == MobSpawnType.EVENT || spawnReason == MobSpawnType.REINFORCEMENT || spawnReason == MobSpawnType.BUCKET || spawnReason == MobSpawnType.DISPENSER || spawnReason == MobSpawnType.PATROL)
			this.setAlbinoStatus(this.getRandom().nextInt(0, 100) == 5 ? true : false);
		return super.finalizeSpawn(world, difficulty, spawnReason, entityData, entityNbt);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.DIRTDRAG0N_IDLE;
	}

	@Override
	protected void playStepSound(BlockPos blockPos, BlockState blockState) {
		this.playSound(ModSounds.GRABOID_MOVING, 1.0F * 0.15f, 0.5F);
	}

	// Mob logic done each tick
	@Override
	public void tick() {
		super.tick();

		// Adds particle effect to surface when moving so you can track it
		var velocityLength = this.getDeltaMovement().horizontalDistance();
		var pos = BlockPos.containing(this.getX(), this.getSurface((int) Math.floor(this.getX()), (int) Math.floor(this.getY()), (int) Math.floor(this.getZ())), this.getZ()).below();
		if (level().getBlockState(pos).isSolidRender(level(), pos) && !this.isDeadOrDying() && this.isInSand())
			if (level().isClientSide && !(velocityLength == 0 && this.getDeltaMovement().horizontalDistance() == 0.0))
				this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(pos)), this.getX(), this.getSurface((int) Math.floor(this.getX()), (int) Math.floor(this.getY()), (int) Math.floor(this.getZ())) + 0.5F, this.getZ(), this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D, this.random.nextGaussian() * 0.02D);

		// Block breaking logic
		if (!this.isDeadOrDying() && this.isAggressive() && !this.isInWater() && this.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) == true) {
			breakingCounter++;
			if (breakingCounter > 10)
				for (BlockPos testPos : BlockPos.betweenClosed(blockPosition().above().relative(getDirection()), blockPosition().relative(getDirection()).above(1))) {
					if (level().getBlockState(testPos).is(AftershockMod.WEAK_BLOCKS) && !level().getBlockState(testPos).isAir()) {
						if (!level().isClientSide)
							this.level().removeBlock(testPos, false);
						if (this.swingingArm != null)
							this.swing(swingingArm);
						breakingCounter = -90;
						if (level().isClientSide())
							this.playSound(SoundEvents.ARMOR_STAND_BREAK, 0.2f + random.nextFloat() * 0.2f, 0.9f + random.nextFloat() * 0.15f);
					}
				}
			if (breakingCounter >= 25)
				breakingCounter = 0;
		}

		// Attack animation logic
		if (attackProgress > 0) {
			attackProgress--;
			if (!level().isClientSide && attackProgress <= 0)
				setCurrentAttackType(AttackType.NONE);
		}
		if (attackProgress == 0 && swinging)
			attackProgress = 10;
		if (!level().isClientSide && getCurrentAttackType() == AttackType.NONE)
			setCurrentAttackType(switch (random.nextInt(2)) {
			case 0 -> AttackType.ATTACK;
			case 1 -> AttackType.HOLD;
			default -> AttackType.ATTACK;
			});
	}
}