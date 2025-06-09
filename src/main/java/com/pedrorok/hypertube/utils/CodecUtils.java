package com.pedrorok.hypertube.utils;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rok, Pedro Lucas nmm. Created on 25/04/2025
 * @project Create Hypertube
 */
public class CodecUtils {

    public static StreamCodec<ByteBuf, Vec3d> VEC3 = new StreamCodec<>() {
        @Override
        public @NotNull Vec3d decode(ByteBuf buffer) {
            return new Vec3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public void encode(ByteBuf buffer, Vec3d value) {
            buffer.writeDouble(value.x);
            buffer.writeDouble(value.y);
            buffer.writeDouble(value.z);
        }

    };

    public static StreamCodec<ByteBuf, List<Vec3d>> VEC3_LIST = new StreamCodec<>() {
        @Override
        public List<Vec3d> decode(ByteBuf byteBuf) {
            int size = byteBuf.readInt();
            List<Vec3d> vec3s = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                vec3s.add(VEC3.decode(byteBuf));
            }
            return vec3s;
        }

        @Override
        public void encode(ByteBuf o, List<Vec3d> vec3s) {
            o.writeInt(vec3s.size());
            for (Vec3d vec3 : vec3s) {
                VEC3.encode(o, vec3);
            }
        }


    };
}
