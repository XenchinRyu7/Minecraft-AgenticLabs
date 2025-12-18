package com.xenchinryu7.mcagenticlabs.entity;

import com.xenchinryu7.mcagenticlabs.action.ActionExecutor;
import com.xenchinryu7.mcagenticlabs.memory.AgentMemory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.DifficultyInstance;
// import net.minecraft.world.entity.MobSpawnType;

import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import java.util.Optional;

public class AgentEntity extends Mob {
    private static final EntityDataAccessor<String> AGENT_NAME =
        SynchedEntityData.defineId(AgentEntity.class, EntityDataSerializers.STRING);

    private String agentName;
    private AgentMemory memory;
    private ActionExecutor actionExecutor;
    private int tickCounter = 0;
    private boolean isFlying = false;
    private boolean isInvulnerable = false;

    public AgentEntity(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
        this.agentName = "Agent";
        this.memory = new AgentMemory(this);
        this.actionExecutor = new ActionExecutor(this);
        this.setCustomNameVisible(true);
        
        this.isInvulnerable = true;
        this.setInvulnerable(true);
    }

    public static AttributeSupplier createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0D)
            .add(Attributes.MOVEMENT_SPEED, 0.25D)
            .add(Attributes.ATTACK_DAMAGE, 8.0D)
            .add(Attributes.FOLLOW_RANGE, 48.0D)
            .build();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(AGENT_NAME, "Agent");
    }

    @Override
    public void tick() {
        super.tick();
        
        if (!this.level().isClientSide()) {
            actionExecutor.tick();
        }
    }

    public void setAgentName(String name) {
        this.agentName = name;
        this.entityData.set(AGENT_NAME, name);
        this.setCustomName(Component.literal(name));
    }

    public String getAgentName() {
        return this.agentName;
    }

    public AgentMemory getMemory() {
        return this.memory;
    }

    public ActionExecutor getActionExecutor() {
        return this.actionExecutor;
    }

    public void addAdditionalSaveData(CompoundTag tag) {
        // super.addAdditionalSaveData(tag);
        tag.putString("agentName", this.agentName);
        
        CompoundTag memoryTag = new CompoundTag();
        this.memory.saveToNBT(memoryTag);
        tag.put("Memory", memoryTag);
    }

    public void readAdditionalSaveData(CompoundTag tag) {
        // super.readAdditionalSaveData(tag);
        if (tag.contains("agentName")) {
            Optional<String> nameOpt = tag.getString("agentName");
            if (nameOpt.isPresent()) {
                this.setAgentName(nameOpt.get());
            }
        }
        
        if (tag.contains("Memory")) {
            Optional<CompoundTag> memoryOpt = tag.getCompound("Memory");
            if (memoryOpt.isPresent()) {
                this.memory.loadFromNBT(memoryOpt.get());
            }
        }
    }

    // public SpawnGroupData finalizeSpawn(ServerLevel level, DifficultyInstance difficulty,
    //                                    MobSpawnType spawnType, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag tag) {
    //     spawnData = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
    //     return spawnData;
    // }

    public void sendChatMessage(String message) {
        if (this.level().isClientSide()) return;
        
        Component chatComponent = Component.literal("<" + this.agentName + "> " + message);
        this.level().players().forEach(player -> ((ServerPlayer)player).sendSystemMessage(chatComponent, false));
    }

    public void setFlying(boolean flying) {
        this.isFlying = flying;
        this.setNoGravity(flying);
        this.setInvulnerableBuilding(flying);
    }

    public boolean isFlying() {
        return this.isFlying;
    }

    /**
     * Set invulnerability for building (immune to ALL damage: fire, lava, suffocation, fall, etc.)
     */
    public void setInvulnerableBuilding(boolean invulnerable) {
        this.isInvulnerable = invulnerable;
        this.setInvulnerable(invulnerable); // Minecraft's built-in invulnerability
    }



    public void travel(net.minecraft.world.phys.Vec3 travelVector) {
        if (this.isFlying && !this.level().isClientSide()) {
            double motionY = this.getDeltaMovement().y;
            
            if (this.getNavigation().isInProgress()) {
                super.travel(travelVector);
                
                // But add ability to move vertically freely
                if (Math.abs(motionY) < 0.1) {
                    // Small upward force to prevent falling
                    this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05, 0));
                }
            } else {
                super.travel(travelVector);
            }
        } else {
            super.travel(travelVector);
        }
    }

    public boolean causeFallDamage(float distance, float damageMultiplier, net.minecraft.world.damagesource.DamageSource source) {
        // No fall damage when flying
        if (this.isFlying) {
            return false;
        }
        return super.causeFallDamage(distance, damageMultiplier, source);
    }

    public void attack(LivingEntity target) {
        if (this.level() instanceof ServerLevel serverLevel) {
            this.doHurtTarget(serverLevel, target);
        }
    }
}

