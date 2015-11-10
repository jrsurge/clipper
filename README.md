Clipper
=======

A SuperCollider utility class for quickly clipping out sounds from soundfiles.

Released under GPL v.3.

What?
-----

* Create a Clipper, giving it the path of a soundfile
* Click and drag to make a selection (Shift + Right-Click to zoom and move around)
* Press spacebar to preview
* Press 'e' to add it as a clip
* Select the list of clips you want to export
* Press the button to open a dialog to save your selected clips (I suggest making a folder - the filename you give will be used as a prefix for exported clips)

How?
----
	(
		Clipper(Platform.resourceDir+/+"sounds/a11wlk01.wav");
	)


TODO
----
* Playhead doesn't update properly - the longer the selection the further out it gets
* Add ability to delete clips from clip list
* Add ability to rename clips - requires rethinking the ListView/Dictionary lookup
