package com.music.bitchord.ui.player

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import com.music.bitchord.data.settings.CoverAnimationMode
import org.intellij.lang.annotations.Language

internal actual val coverMotionShaderSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

@Composable
internal actual fun Modifier.coverMotionShaderEffect(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: () -> Float,
): Modifier {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || mode == CoverAnimationMode.NONE) {
        return this
    }
    return applyAgslCoverEffect(mode = mode, direction = direction, entering = entering, progress = progress)
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun Modifier.applyAgslCoverEffect(
    mode: CoverAnimationMode,
    direction: Int,
    entering: Boolean,
    progress: () -> Float,
): Modifier {
    val isDisintegrateOutgoing = mode == CoverAnimationMode.DISINTEGRATE && !entering
    // One shader instance per layer so outgoing and incoming never overwrite each other's uniforms.
    val shader = remember(isDisintegrateOutgoing) {
        RuntimeShader(
            if (isDisintegrateOutgoing) DISINTEGRATE_OUTGOING_SHADER_SRC else SWEEP_SHADER_SRC,
        )
    }
    val feather = if (mode == CoverAnimationMode.DISINTEGRATE) 0.48f else 0.56f
    val noiseStrength = if (mode == CoverAnimationMode.DISINTEGRATE) 0.24f else 0.12f

    return this.graphicsLayer {
        val t = progress().coerceIn(0f, 1f)
        if (t <= 0.001f || t >= 0.999f || size.width <= 1f || size.height <= 1f) {
            renderEffect = null
            return@graphicsLayer
        }
        shader.setFloatUniform("resolution", size.width, size.height)
        shader.setFloatUniform("progress", t)
        shader.setFloatUniform("direction", direction.toFloat())
        if (!isDisintegrateOutgoing) {
            shader.setFloatUniform("entering", if (entering) 1f else 0f)
            shader.setFloatUniform("feather", feather)
            shader.setFloatUniform("noiseStrength", noiseStrength)
        }
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, "composable").asComposeRenderEffect()
    }
}

@Language("AGSL")
private const val SWEEP_SHADER_SRC = """
uniform shader composable;
uniform float2 resolution;
uniform float progress;
uniform float direction;
uniform float entering;
uniform float feather;
uniform float noiseStrength;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(resolution, float2(1.0));
    float coord;
    if (abs(direction) > 0.5) {
        coord = direction > 0.0 ? clamp(1.0 - uv.x, 0.0, 1.0) : clamp(uv.x, 0.0, 1.0);
    } else {
        float maxHalf = 0.5 * length(resolution);
        coord = clamp(length(fragCoord - 0.5 * resolution) / max(maxHalf, 1.0), 0.0, 1.0);
    }

    float pad = 0.5 * noiseStrength + 0.02;
    float front = mix(-pad, 1.0 + feather + pad, progress);
    float n = (vnoise(uv * float2(4.5, 4.5)) - 0.5) * noiseStrength;
    float sweep = smoothstep(front - feather, front, coord + n);
    float mask = entering > 0.5 ? (1.0 - sweep) : sweep;

    float2 sampleCoord = fragCoord;
    if (entering <= 0.5 && abs(direction) > 0.5) {
        float edgeDrift = (1.0 - mask) * direction * resolution.x * 0.014;
        sampleCoord.x = clamp(fragCoord.x + edgeDrift, 0.0, max(resolution.x - 1.0, 0.0));
    }

    half4 color = composable.eval(sampleCoord);
    return half4(color.rgb * mask, color.a * mask);
}
"""

@Language("AGSL")
private const val DISINTEGRATE_OUTGOING_SHADER_SRC = """
uniform shader composable;
uniform float2 resolution;
uniform float progress;
uniform float direction;

float hash21(float2 p) {
    p = fract(p * float2(127.1, 311.7));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.52;
    for (int i = 0; i < 3; i++) {
        v += amp * vnoise(p);
        p *= 2.07;
        amp *= 0.48;
    }
    return clamp(v, 0.0, 1.0);
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(resolution, float2(1.0));
    float coord;
    float2 driftDir;
    if (abs(direction) > 0.5) {
        coord = direction > 0.0 ? clamp(1.0 - uv.x, 0.0, 1.0) : clamp(uv.x, 0.0, 1.0);
        driftDir = float2(direction, 0.0);
    } else {
        float maxHalf = 0.5 * length(resolution);
        float2 offset = fragCoord - 0.5 * resolution;
        float dist = length(offset);
        coord = clamp(dist / max(maxHalf, 1.0), 0.0, 1.0);
        driftDir = dist > 1.0 ? -(offset / dist) : float2(0.0, -1.0);
    }

    float fbmVal = fbm(uv * float2(11.0, 11.0));
    float threshold = coord * 0.70 + fbmVal * 0.30;
    float erosionWidth = 0.34;
    float wave = progress * (1.0 + erosionWidth);
    float delta = wave - threshold;

    if (delta <= 0.0) {
        return composable.eval(fragCoord);
    }

    float e = clamp(delta / erosionWidth, 0.0, 1.0);
    float2 curl = float2(
        driftDir.x * e * resolution.x * 0.045,
        (sin(uv.x * 16.0 + fbmVal * 6.0) * 0.5 - 0.35 + driftDir.y * 0.4) * e * resolution.y * 0.032
    );
    float2 displaced = clamp(fragCoord + curl, float2(0.0), max(resolution - float2(1.0), float2(0.0)));
    half4 color = composable.eval(displaced);

    float grain = hash21(floor(fragCoord / 3.0));
    float grainVal = grain * 0.72 + fbmVal * 0.28 + 0.08;
    float dissolveCut = e * 1.08;
    float grainMask = smoothstep(dissolveCut - 0.20, dissolveCut + 0.04, grainVal) * (1.0 - e * e);

    float rim = smoothstep(0.0, 0.08, e) * (1.0 - smoothstep(0.08, 0.25, e));
    float3 rimGlow = (color.rgb * 0.45 + float3(0.18, 0.13, 0.09) * color.a) * rim;
    half3 outRgb = min(half3(color.rgb + rimGlow), half3(color.a));

    return half4(outRgb * grainMask, color.a * grainMask);
}
"""
