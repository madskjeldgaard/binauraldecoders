BinauralDecoderCIPIC : PersistentMainFX{
  classvar <order, <subjectID;
  classvar <binauralDecoder;

  *new {|hoaOrder=3, id=21|
    order = hoaOrder;
    subjectID = id;

    ^this.init();
  }

  *prepareResources{
    // @TODO this should actually be freed after usage, but as it's persistent until recompile I guess it doesn't matter too much
    binauralDecoder = FoaDecoderKernel.newCIPIC(subjectID);
  }

  *synthFunc{
    ^{|out=0, yaw=0, pitch=0, roll=0, bypass=0|

        // HOA input
        var hoaIn = In.ar(out, numChans);
        var hoa = hoaIn.keep(order.asHoaOrder.size);

        // format exchange: HOA >> FOA
        var lowCutFreq = 30.0;  // highpass freq

        // design encoder to exchange (ordering, normalisation)
        var encoder = FoaEncoderMatrix.newHoa1;

        var foa, stereo, sig;

        // Rotate scene
        hoa = HoaYPR.ar(in: hoa,  yaw: yaw,  pitch: pitch,  roll: roll,  order: order);

        // exchange (ordering, normalisation)
        // truncate to HOA1
        foa = FoaEncode.ar(hoa.keep(AtkFoa.defaultOrder.asHoaOrder.size), encoder);

        // pre-condition FOA to make it work with FoaProximity
        foa = HPF.ar(foa, lowCutFreq);

        // Exchange reference radius
        foa = FoaProximity.ar(foa, AtkHoa.refRadius);

        // Decode to binaural
        stereo = FoaDecode.ar(in: foa,  decoder: binauralDecoder);

        // Pad output with silence after the stereo channels
        stereo = stereo ++ Silent.ar().dup(numChans-2);

        sig = Select.ar(which: bypass,  array: [stereo, hoaIn]);

        ReplaceOut.ar(bus: out,  channelsArray: sig);
      }
  }
}

/*

BinauralDecoderIEM : PersistentMainFX{
  classvar <order;
  classvar <vstController;
  classvar <iemBinDecRefRadius = 3.25;

  *new {|hoaOrder=3|
    order = hoaOrder;

    ^this.init();
  }

  *gui{
    vstController.sceneRot.gui();
    vstController.binauralDec.gui();
  }

  *editor{
    vstController.sceneRot.editor();
    vstController.binauralDec.editor();
  }

  *prepareResources{
    forkIfNeeded{
      vstController = VSTPluginController.collect(synth);
      Server.local.sync;
      vstController.sceneRot.open("SceneRotator");
      Server.local.sync;
      vstController.binauralDec.open("BinauralDecoder");
    }
  }

  *synthFunc{
    ^{|bus, bypass=0|

        // HOA input
        var sig = In.ar(bus, numChans);

        // Format exchange from ATK's HOA-format to what IEM expects (ambix) with the binauralDecoder's expected radius.
        // (for source, see https://github.com/ambisonictoolkit/atk-sc3/issues/95)
        // exchange reference radius
        sig = HoaNFCtrl.ar(
          in: sig,
          encRadius: AtkHoa.refRadius,
          decRadius: iemBinDecRefRadius,
          order: order
        );

        // exchange normalisation
        sig = HoaDecodeMatrix.ar(
          in: sig,
          hoaMatrix: HoaMatrixDecoder.newFormat(\ambix, order)
        );

        /*
        the resulting signal can be
        fed directly to the IEM BinauralDecoder plugin
        and is encoded as:

        Ambisonic order: order
        Ambisonic component ordering: ACN
        Ambisonic component normalisation: SN3D
        Ambisonic reference radius: 3.25
        */

        // This will be the SceneRotator
        sig = VSTPlugin.ar(sig, numChans, id: \sceneRot, bypass: bypass);

        // This will be the BinauralDecoder
        sig = VSTPlugin.ar(sig, numChans, id: \binauralDec, bypass: bypass);

        // Pad output with silence after the stereo channels
        sig = sig ++ Silent.ar().dup(numChans-2);

        ReplaceOut.ar(bus, sig);
      }
  }
}
*/
