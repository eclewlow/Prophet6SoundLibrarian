# Prophet6SoundLibrarian
Prophet 6 Sound Librarian for organizing patches on the Sequential Circuits Prophet 6

[![License](https://img.shields.io/badge/License-Eclipse%20Public%20License%202.0-blue.svg)](#license)

## Installation

### Standalone Prophet 6 Sound Librarian download

You can download the standalone jar
from the
[releases](https://github.com/eclewlow/Prophet6SoundLibrarian/releases) page.
[![jar](https://img.shields.io/github/downloads/eclewlow/Prophet6SoundLibrarian/total.svg)](https://github.com/eclewlow/Prophet6SoundLibrarian/releases)

Then simply double click the JAR file and the Prophet6SoundLibrarian window should appear

## Using Prophet6SoundLibrarian

On opening the Prophet6SoundLibrarian JAR file, the main window should appear as below.

<img width="720" alt="Screen Shot 2022-10-19 at 2 42 14 AM" src="https://user-images.githubusercontent.com/32854625/196656599-d5dc152b-a263-4934-a2b5-3d4c8b69deb9.png">

The Prophet6SoundLibrarian allows for transferring user bank/programs to and from the Prophet 6 synthesizer.
It also allows for reordering single patches or group-selected patches, renaming patches, saving an entire 500-patch library (`*.p6lib` file), saving single or multiple-selected programs (`*.p6program` file), and loading both `p6lib` and `p6program` files.

### Connecting the Sequential Circuits Prophet 6

To use, connect the Sequential Circuits Prophet 6 synthesizer to the computer via USB cable.

Make sure MIDI SysEx is set to USB on the Prophet 6.


Upon connecting the Prophet 6, the connection status in the TRANSFER area should change from "DISCONNECTED" to "CONNECTED", and "No Device" should change to "Prophet 6".

Also, the buttons in the TRANSFER area will change from disabled to enabled.

### Transferring Patches between the Prophet 6 and Prophet6SoundLibrarian

Click the "Receive All" button to read all 500 user bank patches from the Prophet 6.  This will overwrite the current library being displayed in the PATCH LIST area.

Click the "Send All" button to write all 500 patches in the current library to the Prophet 6.  This will overwrite all Prophet 6 user banks with current library.

Click the "Receive" button while the PATCH LIST table has rows selected to read the selected Bank/Programs from 

Click the "Send" button
