Clipper{
	var prSoundFile, prBuffer, prWin, prSynth, prLayout, prSoundFileView, prPlayHeadRoutine, prClipList, prClipListView;

	*new{ | file="" |
		^super.newCopyArgs(file).init;
	}

	init{
		prWin = Window("Clipper").onClose_({ prBuffer.free; prClipList.clear; if(prSynth.isPlaying) {prSynth.free; AppClock.clear}; });
		prLayout = VLayout();

		prSoundFileView = SoundFileView();

		this.prSynthDefInit;
		this.prSoundFileInit(prSoundFile);
		this.prSoundFileViewInit;
		this.prClipListViewInit;

		prClipList = Dictionary();

		prLayout.add(prSoundFileView);
		prLayout.add(prClipListView);

		prLayout.add(
			Button()
			.states_([["Write selected clips to file"]])
			.action_({
				Dialog.savePanel({ | path |
					prClipListView.selection.do({ | v |
						var selection = prClipList[prClipListView.items[v]];
						prBuffer.write(path ++ "-" ++ selection[0] ++ "_" ++ selection[1] ++ ".wav","wav","int24",selection[1],selection[0]);
					});
				});
			})
		);

		prLayout.add(nil);

		prWin.layout_(prLayout);
		prWin.front();
	}

	prSoundFileInit{ | path |
		prBuffer.free;
		prClipList.clear;
		SoundFile.use(path,{ |file|
			prSoundFileView
			.soundfile_(file)
			.readWithTask(0,file.numFrames,512,{},true);
			prBuffer = file.asBuffer(Server.default);
			prBuffer.bufnum;
		});
	}

	prSoundFileViewInit{
		prSoundFileView
		.timeCursorOn_(true)
		.timeCursorPosition_(0)
		.gridOn_(true)
		.gridResolution_(5)
		.keyDownAction_({ | caller, modifiers, unicode, keycode|
			var start = prSoundFileView.selections[0][0];
			var range = prSoundFileView.selections[0][1];

			if(range == 0)
			{
				range = prBuffer.numFrames - start;
			};

			case
			{keycode == 32} { this.playCurrentSelection; }
			{keycode == 13} { caller.timeCursorPosition_(0); caller.setSelection(0,[0,0]); }
			{keycode == 101} {
				this.prAddClip(start,range) }
			;
		})
		;
	}

	prClipListViewInit{
		prClipListView = ListView()
		.selectionMode_(\extended)
		.action_({ | caller |
			this.updateCurrentSelection(caller.items[caller.selection[0]]);
		})
		.keyDownAction_({ | caller, modifiers, unicode, keycode |
			case
			{ keycode == 32 } { this.playCurrentSelection }
			;
		})
		;
	}

	prSynthDefInit{
		SynthDef(\jrs_clipperSynth_stereo,{ | buffer=0, start=0, range=1, gate=1 |
			var sig, env;

			env = EnvGen.kr(Env([1,1],[range/SampleRate.ir]),1,1,0,1,2);
			sig = BufRd.ar(2,buffer, Phasor.ar(1,BufRateScale.kr(buffer),start,start+range),0);
			Out.ar(0,sig);
		}).add;
		SynthDef(\jrs_clipperSynth_mono,{ | buffer=0, start=0, range=1, gate=1 |
			var sig, env;

			env = EnvGen.kr(Env([1,1],[range/SampleRate.ir]),1,1,0,1,2);
			sig = BufRd.ar(1,buffer, Phasor.ar(1,BufRateScale.kr(buffer),start,start+range),0);
			Out.ar(0,sig!2);
		}).add;
	}

	prUpdatePlayhead{ | start, range |
		prPlayHeadRoutine = Routine({
			var updateRes = 0.01;
			var updateRatio = 44100 * updateRes;
			var numUpdates, remainder;

			prSoundFileView.timeCursorPosition_(start);

			numUpdates = (range/updateRatio.asFloat -1);
			remainder = (range/updateRatio.asFloat -1) - numUpdates;

			(numUpdates).do({
				prSoundFileView.timeCursorPosition_(prSoundFileView.timeCursorPosition + updateRatio + remainder);
				(updateRes).wait;
			});
			prSoundFileView.timeCursorPosition_(start);
			prSynth.isPlaying_(false);
		});
		AppClock.play(prPlayHeadRoutine);
	}

	prAddClip{ | start, range |
		prClipList.add(\clip++start++"_"++range -> [start,range]);
		this.updateClipListView;
	}

	updateClipListView{
		prClipListView.items_(prClipList.keys.asArray.sort);
	}

	updateCurrentSelection{| key |
		var selection;

		selection = prClipList[key];
		prSoundFileView.setSelection(0,selection);
		prSoundFileView.timeCursorPosition_(selection[0]);
	}

	playCurrentSelection{
		var start = prSoundFileView.selections[0][0];
		var range = prSoundFileView.selections[0][1];

		if(prSynth.isPlaying) { prSynth.free; AppClock.clear(); prSoundFileView.timeCursorPosition_(start)} {

			if(range == 0)
			{
				range = prBuffer.numFrames - start;
			};

			if(prBuffer.numChannels == 2)
			{
				prSynth = Synth(\jrs_clipperSynth_stereo, [\buffer, prBuffer, \start, start, \range, range]);
			}
			{
				prSynth = Synth(\jrs_clipperSynth_mono, [\buffer, prBuffer, \start, start, \range, range]);
			};
			prSynth.isPlaying_(true);
			this.prUpdatePlayhead(start,range);
		}
	}
}