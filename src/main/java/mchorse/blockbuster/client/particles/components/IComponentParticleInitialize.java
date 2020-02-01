package mchorse.blockbuster.client.particles.components;

import mchorse.blockbuster.client.particles.emitter.BedrockEmitter;
import mchorse.blockbuster.client.particles.emitter.BedrockParticle;

public interface IComponentParticleInitialize
{
	public void apply(BedrockEmitter emitter, BedrockParticle particle);
}