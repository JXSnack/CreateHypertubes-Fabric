package com.pedrorok.hypertube.managers.sound;

import lombok.Setter;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

@Setter
public class TubeSound extends MovingSoundInstance {

	private float pitch;

	public TubeSound(SoundEvent soundEvent, float pitch) {
		super(soundEvent, SoundCategory.BLOCKS, SoundInstance.createRandom());
		this.pitch = pitch;
		volume = 0.01f;
		repeat = true;
		repeatDelay = 0;
		relative = true;
	}

	@Override
	public void tick() {}

    public void fadeIn(float maxVolume) {
		volume = Math.min(maxVolume, volume + .05f);
	}

	public void fadeOut() {
		volume = Math.max(0, volume - 0.05f);
	}

	public boolean isFaded() {
		return volume == 0;
	}

	@Override
	public float getPitch() {
		return pitch;
	}

	public void stopSound() {
		setDone();
	}

	public void updateLocation(Vec3d pos) {
		x = pos.x;
		y = pos.y;
		z = pos.z;
	}

	@Override
	public boolean isRelative() {
		return true;
	}
}