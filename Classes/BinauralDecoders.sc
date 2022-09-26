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

        var hoaIn = In.ar(out, order.asHoaOrder.size);
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
        stereo = stereo ++ Silent.ar().dup(order.asHoaOrder.size-2);

        sig = Select.ar(which: bypass,  array: [stereo, hoaIn]);

        ReplaceOut.ar(bus: out,  channelsArray: sig);
      }
  }
}

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

  *afterSynthInit{
    forkIfNeeded{
      if(\VSTPluginController.asClass.notNil,{
        vstController = \VSTPluginController.asClass.collect(synth);
        Server.local.sync;
        vstController.sceneRot.open("SceneRotator");
        Server.local.sync;
        vstController.binauralDec.open("BinauralDecoder");
      })
    }
  }

  *synthFunc{
    ^{|bus, bypass=0|
        var binaural;

        // HOA input
        var hoaIn = In.ar(bus, order.asHoaOrder.size);
        var hoa = hoaIn.keep(order.asHoaOrder.size);

        // Format exchange from ATK's HOA-format to what IEM expects (ambix) with the binauralDecoder's expected radius.
        // (for source, see https://github.com/ambisonictoolkit/atk-sc3/issues/95)
        // exchange reference radius
        hoa = HoaNFCtrl.ar(
          in: hoa,
          encRadius: AtkHoa.refRadius,
          decRadius: iemBinDecRefRadius,
          order: order
        );

        // exchange normalisation
        hoa = HoaDecodeMatrix.ar(
          in: hoa,
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

        if(\VSTPlugin.asClass.notNil,{
          // This will be the SceneRotator
          hoa = \VSTPlugin.asClass.ar(hoa, order.asHoaOrder.size, id: \sceneRot, bypass: bypass);

          // This will be the BinauralDecoder
          binaural = \VSTPlugin.asClass.ar(hoa, order.asHoaOrder.size, id: \binauralDec, bypass: bypass);
        });

        // Pad output with silence after the stereo channels
        binaural = binaural ++ Silent.ar().dup(order.asHoaOrder.size-2);

        ReplaceOut.ar(bus, binaural);
      }
  }
}
