# BinauralDecoders

This repository contains easy to use binaural stereo decoders for high order ambisonics using [the ambisonic toolkit for SuperCollider](https://www.ambisonictoolkit.net/) that are automatically set up as persistent main effects on the main outputs of SuperCollider. They are respawned when the user hard stops the sound.

This quark depends on the ambisonic toolkit and requires a full installation of it. See [these instructions](https://github.com/ambisonictoolkit/atk-sc3#installing) for more information. 

It also depends on [persistentmainfx](https://github.com/madskjeldgaard/persistentmainfx) which is installed automatically by the quark system.

If you want to use the IEM based binaural decoder, you need the [vstplugin extension](https://git.iem.at/pd/vstplugin/-/releases) and the [IEM Plugins](https://plugins.iem.at/docs/).


### Installation

Open up SuperCollider and evaluate the following line of code:
`Quarks.install("https://github.com/madskjeldgaard/binauraldecoders")`

## Example usage

```supercollider
// Setup decoder
(
~order = ~order ? 3;
BinauralDecoderCIPIC.new(hoaOrder: ~order);
)

// Inspect the node tree to see that it is in fact in action:
s.plotTree

// Play some high order ambisonics sound
(
// White Noise going round
Ndef(\hoa_testorientation, {|amp=0.125, freq=100, rotFreq=1, ele=0|
	// var sig = LFTri.ar(freq)*amp;
	var sig = WhiteNoise.ar(amp);
	var azi = LFSaw.kr(rotFreq, mul: pi);

	sig = HoaEncodeDirection.ar(
		sig,
		azi,
		ele,
		AtkHoa.refRadius,
		~order
	);

}).play
)

// Bypass the decoder to output raw ambisonics
BinauralDecoderCIPIC.set(\bypass, 1);
```
