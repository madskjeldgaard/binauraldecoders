BinauralDecodersTest1 : UnitTest {
	test_check_classname {
		var result = BinauralDecoders.new;
		this.assert(result.class == BinauralDecoders);
	}
}


BinauralDecodersTester {
	*new {
		^super.new.init();
	}

	init {
		BinauralDecodersTest1.run;
	}
}
