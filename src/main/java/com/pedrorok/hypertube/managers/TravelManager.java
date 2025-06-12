package com.pedrorok.hypertube.managers;

import com.pedrorok.hypertube.HypertubeMod;
import com.pedrorok.hypertube.blocks.HyperEntranceBlock;
import com.pedrorok.hypertube.config.ClientConfig;
import com.pedrorok.hypertube.events.PlayerSyncEvents;
import com.pedrorok.hypertube.managers.sound.TubeSoundManager;
import com.pedrorok.hypertube.registry.ModSounds;
import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Rok, Pedro Lucas nmm. Created on 22/04/2025
 * @project Create Hypertube
 */
public class TravelManager {

    public static final String TRAVEL_TAG = "hypertube_travel";
    public static final String LAST_TRAVEL_TIME = "last_travel_time";

    public static final String LAST_TRAVEL_BLOCKPOS = "last_travel_blockpos";
    public static final String LAST_TRAVEL_SPEED = "last_travel_speed";
    public static final String LAST_POSITION = "last_travel_position";

    public static final int DEFAULT_TRAVEL_TIME = 2000;
    public static final int DEFAULT_AFTER_TUBE_CAMERA = 1500; // 0.5 seconds (subtracting default travel time)

    private static final Map<UUID, TravelData> travelDataMap = new HashMap<>();

    public static void tryStartTravel(ServerPlayerEntity player, BlockPos pos, BlockState state, float speed) {
        NbtCompound playerPersistData = player.getPersistentData();
        if (playerPersistData.getBoolean(TRAVEL_TAG)) return;
        long lastTravelTime = playerPersistData.getLong(LAST_TRAVEL_TIME);

        if (playerPersistData.contains(LAST_TRAVEL_BLOCKPOS)) {
            BlockPos lastTravelPos = BlockPos.fromLong(playerPersistData.getLong(LAST_TRAVEL_BLOCKPOS));
            if (lastTravelPos.equals(pos)
                && lastTravelTime > System.currentTimeMillis()) {
                return;
            }
        }

        if (lastTravelTime - TravelManager.DEFAULT_AFTER_TUBE_CAMERA > System.currentTimeMillis()) {
            speed += playerPersistData.getFloat(LAST_TRAVEL_SPEED); // Increase speed if player is trying to fast travel
        }

        BlockPos relative = pos.offset(state.get(HyperEntranceBlock.FACING));
        TravelData travelData = new TravelData(relative, player.getWorld(), pos, speed);

        if (travelData.getTravelPoints().size() < 3) {
            // TODO: Handle error
            return;
        }

        playerPersistData.putBoolean(TRAVEL_TAG, true);
        AllPackets.getChannel().send(
                PacketDistributor.PLAYER.with(() -> player),
                new ISyncPersistentData.PersistentDataPacket(player)
        );

        HypertubeMod.LOGGER.debug("Player start travel: {} to {} and speed {}", player.getName().getString(), relative, travelData.getSpeed());
        travelDataMap.put(player.getUuid(), travelData);
        player.setNoGravity(true);
        PlayerSyncEvents.syncPlayerStateToAll(player);

        Vec3d center = pos.toCenterPos();

        Vec3d eyePos = player.getEyePos();
        Vec3d playerPos = player.getPos();
        if (playerPos.distanceTo(center) > eyePos.distanceTo(center)) {
            player.requestTeleportOffset(0, 1, 0);
        }

        playHypertubeSuctionSound(player, center);
    }

    public static void playerTick(PlayerEntity player) {
        handleCommon(player);
        if (player.getWorld().isClient) {
            clientTick(player);
            return;
        }
        handleServer(player);
    }

    private static void handleCommon(PlayerEntity player) {
        if (hasHyperTubeData(player)) {
            player.calculateDimensions();
        }
    }

    private static boolean isTraveling;

    @Environment(EnvType.CLIENT)
    private static void clientTick(PlayerEntity player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!mc.player.isPartOf(player)) return;

        if (hasHyperTubeData(player)) {
            TubeSoundManager.TravelSound.enableClientPlayerSound(player, 0.8F, 1.0F);
            isTraveling = true;
            return;
        }
        if (isTraveling
            && !ClientConfig.get().ALLOW_FPV_INSIDE_TUBE.get()) {
            MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
            isTraveling = false;
        }
    }

    private static void finishTravel(ServerPlayerEntity player, TravelData travelData) {

        PlayerSyncEvents.syncPlayerStateToAll(player);

        travelDataMap.remove(player.getUuid());
        player.getPersistentData().putBoolean(TRAVEL_TAG, false);
        // --- NOTE: this is just to make easy to debug
        player.getPersistentData().putLong(LAST_TRAVEL_TIME, System.currentTimeMillis() + DEFAULT_TRAVEL_TIME);
        player.getPersistentData().putLong(LAST_TRAVEL_BLOCKPOS, travelData.getLastBlockPos().asLong());
        player.getPersistentData().putFloat(LAST_TRAVEL_SPEED, travelData.getSpeed());
        // ---
        AllPackets.getChannel().send(
                PacketDistributor.ALL.noArg(),
                new ISyncPersistentData.PersistentDataPacket(player)
        );

        Vec3d lastDir = travelData.getLastDir();
        Vec3d lastBlockPos = travelData.getLastBlockPos().toCenterPos();
        player.teleport((ServerWorld) player.getWorld(), lastBlockPos.x, lastBlockPos.y, lastBlockPos.z, player.getYaw(), player.getPitch());
        player.requestTeleportOffset(lastDir.x, lastDir.y, lastDir.z);
        player.setPose(EntityPose.CROUCHING);
        player.setVelocity(travelData.getLastDir().multiply(travelData.getSpeed() + 0.5));
        player.velocityModified = true;
        player.setNoGravity(false);
        PlayerSyncEvents.syncPlayerStateToAll(player);

        playHypertubeSuctionSound(player, player.getPos());
    }

    private static void handleServer(PlayerEntity player) {
        if (!travelDataMap.containsKey(player.getUuid())) {
            if (!player.getPersistentData().getBoolean(TRAVEL_TAG)) return;
            player.getPersistentData().putBoolean(TRAVEL_TAG, false);
            player.setNoGravity(false);
            return;
        }
        handlePlayerTraveling(player);
    }

    private static void handlePlayerTraveling(PlayerEntity player) {
        TravelData travelData = travelDataMap.get(player.getUuid());
        Vec3d currentPoint = travelData.getTravelPoint();

        if (travelData.isFinished()) {
            finishTravel((ServerPlayerEntity) player, travelData);
            return;
        }

        currentPoint = currentPoint.subtract(0, 0.25, 0);
        Vec3d playerPos = player.getPos();
        double speed = 0.5D + travelData.getSpeed();

        Vec3d nextPoint = getNextPointPreview(travelData, 0);
        if (nextPoint == null) {
            Vec3d direction = currentPoint.subtract(playerPos).normalize();
            player.setVelocity(direction.multiply(speed));
            player.velocityModified = true;
            return;
        }

        nextPoint = nextPoint.subtract(0, 0.25, 0);

        Vec3d segmentDirection = nextPoint.subtract(currentPoint).normalize();
        double segmentLength = currentPoint.distanceTo(nextPoint);

        Vec3d toPlayer = playerPos.subtract(currentPoint);
        double currentProjection = toPlayer.dotProduct(segmentDirection);
        currentProjection = Math.max(0, Math.min(segmentLength, currentProjection));

        Vec3d currentIdealPosition = currentPoint.add(segmentDirection.multiply(currentProjection));

        double nextProjection = currentProjection + speed;

        Vec3d targetPosition;
        Vec3d finalDirection;
        boolean shouldAdvanceWaypoint = false;

        if (nextProjection >= segmentLength * 0.95) {
            shouldAdvanceWaypoint = true;

            Vec3d nextNextPoint = getNextPointPreview(travelData, 1);
            if (nextNextPoint != null) {
                nextNextPoint = nextNextPoint.subtract(0, 0.25, 0);

                double overflow = nextProjection - segmentLength;

                Vec3d nextSegmentDirection = nextNextPoint.subtract(nextPoint).normalize();

                targetPosition = nextPoint.add(nextSegmentDirection.multiply(overflow));

                double transitionFactor = Math.min(1.0, (nextProjection - segmentLength * 0.8) / (segmentLength * 0.2));
                finalDirection = segmentDirection.add(nextSegmentDirection.subtract(segmentDirection).multiply(transitionFactor)).normalize();
            } else {
                targetPosition = nextPoint;
                finalDirection = segmentDirection;
            }
        } else {
            targetPosition = currentPoint.add(segmentDirection.multiply(nextProjection));
            finalDirection = segmentDirection;
        }

        Vec3d idealMovement = targetPosition.subtract(currentIdealPosition);
        Vec3d actualMovement = targetPosition.subtract(playerPos);

        double distanceFromLine = playerPos.distanceTo(currentIdealPosition);
        double correctionStrength = Math.min(1.0, distanceFromLine * 2.0);

        Vec3d correctedMovement = idealMovement.add(actualMovement.subtract(idealMovement).multiply(correctionStrength));

        if (correctedMovement.length() > 0.001) {
            Vec3d movementDirection = correctedMovement.normalize();

            double smoothingFactor = Math.max(0.3, 0.5 - distanceFromLine);
            movementDirection = movementDirection.add(finalDirection.subtract(movementDirection).multiply(smoothingFactor)).normalize();

            player.setVelocity(movementDirection.multiply(speed));
        } else {
            player.setVelocity(finalDirection.multiply(speed));
        }

        if (shouldAdvanceWaypoint) {
            travelData.getNextTravelPoint();
            if (travelData.getTravelPoint() != null) {
                Vec3d newNextPoint = getNextPointPreview(travelData, 0);
                if (newNextPoint != null) {
                    Vec3d newDirection = newNextPoint.subtract(travelData.getTravelPoint()).normalize();
                    travelData.setLastDir(newDirection);
                }
            }
        }
        checkAndCorrectStuck(player, travelData);

        player.velocityModified = true;
    }


    private static void checkAndCorrectStuck(PlayerEntity player, TravelData travelData) {
        if (!travelData.hasNextTravelPoint()) return;
        if (player.age % 5 != 0) return;

        float x = player.getPersistentData().getFloat(LAST_POSITION + "_x");
        float y = player.getPersistentData().getFloat(LAST_POSITION + "_y");
        float z = player.getPersistentData().getFloat(LAST_POSITION + "_z");
        Vec3d lastPosition = new Vec3d(x, y, z);


        if (player.getPos().distanceTo(lastPosition) < 0.01) {
            // player is stuck
            travelData.getNextTravelPoint();
            Vec3d travelPoint = travelData.getTravelPoint();
            player.teleport(travelPoint.x, travelPoint.y, travelPoint.z);
            return;
        }
        player.getPersistentData().putFloat(LAST_POSITION + "_x", (float) player.getPos().x);
        player.getPersistentData().putFloat(LAST_POSITION + "_y", (float) player.getPos().y);
        player.getPersistentData().putFloat(LAST_POSITION + "_z", (float) player.getPos().z);
    }

    private static Vec3d getNextPointPreview(TravelData travelData, int offset) {
        List<Vec3d> points = travelData.getTravelPoints();
        int currentIndex = travelData.getTravelIndex();
        int targetIndex = currentIndex + 1 + offset;

        if (targetIndex < points.size()) {
            return points.get(targetIndex);
        }
        travelData.setFinished(true);
        return null;
    }

    private static void playHypertubeSuctionSound(ServerPlayerEntity player, Vec3d pos) {
        Random random = player.getWorld().random;
        float pitch = 0.8F + random.nextFloat() * 0.4F;
        int seed = random.nextInt(1000);
        for (PlayerEntity oPlayer : player.getWorld().getPlayers()) {
            ((ServerPlayerEntity) oPlayer).networkHandler.sendPacket(new PlaySoundS2CPacket(
                    // Added Registries.SOUND_EVENT.getEntry, since we use different registering than the Forge version
                    Registries.SOUND_EVENT.getEntry(ModSounds.HYPERTUBE_SUCTION),
                    SoundCategory.BLOCKS, pos.x, pos.y, pos.z, 1, pitch, seed));
        }
    }

    public static boolean hasHyperTubeData(Entity player) {
        return player.getPersistentData().getBoolean(TRAVEL_TAG);
    }
}
