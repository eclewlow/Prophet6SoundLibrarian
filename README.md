# Prophet6SoundLibrarian
Prophet 6 Sound Librarian software for organizing Sequential Circuits Prophet 6 patches.

[![License](https://img.shields.io/badge/License-Eclipse%20Public%20License%202.0-blue.svg)](https://www.eclipse.org/legal/epl-2.0/)

Prophet6SoundLibrarian is made possible through the Mac Java MIDI SysEx implementation by Derek Cook.  The library can be found here https://github.com/DerekCook/CoreMidi4J.  CoreMidi4J is provided similarly under the Eclipse Public License 1.0.

## System Requirements

Requirements as far as I know:
- Up-to-date Java (https://www.java.com/en/download/manual.jsp)

I've only tested this on my Mac.  It may work on other systems.

## Installation

### Standalone Prophet 6 Sound Librarian download

1. You can download the standalone jar
from the
[releases](https://github.com/eclewlow/Prophet6SoundLibrarian/releases) page.
[![jar](https://img.shields.io/github/downloads/eclewlow/Prophet6SoundLibrarian/total.svg)](https://github.com/eclewlow/Prophet6SoundLibrarian/releases)

2. Double click the JAR file.
3. You may have to open System Settings -> Security to allow the application to be opened.
4. The Prophet6SoundLibrarian window should appear.

## Using Prophet6SoundLibrarian

On opening the Prophet6SoundLibrarian JAR file, the main window should appear as below.

![image](https://user-images.githubusercontent.com/32854625/202584333-08a25218-04a4-4299-96dc-49829984001a.png)

The Prophet6SoundLibrarian allows for transferring user bank/programs to and from the Prophet 6 synthesizer.  Currently supported files for merge are .syx and .p6lib (a Prophet6SoundLibrarian format file) files.
It also allows for reordering single patches or group-selected patches, renaming patches, saving an entire 500-user-patch library (`*.p6lib` file), saving single or multiple-selected programs (`*.p6program` file), and loading both `p6lib` and `p6program` files.  It also allows for merging a .p6lib library or Prophet 6 .syx file into the current working library. 

### Connecting the Sequential Circuits Prophet 6

To use, connect the Sequential Circuits Prophet 6 synthesizer to the computer via USB cable.

Make sure MIDI SysEx is set to USB on the Prophet 6.


Upon connecting the Prophet 6, the connection status in the TRANSFER area should change from "DISCONNECTED" to "CONNECTED", and "No Device" should change to "Prophet 6".

Also, the buttons in the TRANSFER area will change from disabled to enabled.

### Best Workflow

1. Open Prophet6SoundLibrarian
2. Click Receive All (this will read all user banks from Prophet 6)
3. Choose File->Save Library... (this will create a backup of the Prophet 6 user banks)
4. It is now safe to freely edit without worrying about your most recent Prophet 6 user banks, as you can easily load them from the `.p6lib` backup you just created and click Send All to restore the Prophet 6 user banks.
5. Happy editing!

### PROGRAMS area

- `Receive All` - read all 500 user bank patches from the Prophet 6.  `IMPORTANT!`  This will overwrite the entire current working library.

- `Send All` - write all 500 patches in the current library to the Prophet 6.  `IMPORTANT!` This will overwrite all 500 Prophet 6 user banks with the current working library.

- `Receive` - receive from the connected Prophet 6 the currently selected working library bank/programs. `IMPORTANT!` This will overwrite the selected user banks in the current working library.

- `Send` - write to the connected Prophet 6 the currently selected working library bank/programs.  `IMPORTANT!` This will write the currently selected bank/programs to the respective Prophet 6 bank/program numbers.


### AUDITION area

- `Send` - send the currently selected working library patch to the Prophet 6 edit buffer.  This will allow you to play the patch on the Prophet 6 without overwriting any of the bank/programs.


### Print Dialog

- A File->Print... option was added in v1.1.12.  The dialog may also have an export to PDF option.  The result is shown below.  Known issue: on Mac, the "Open in Preview" button functionality is broken as per the following ticket.  Clicking on "Open in Preview" will send the job to the printer. (https://bugs.openjdk.org/browse/JDK-8276027).

![image](https://user-images.githubusercontent.com/32854625/202584350-eed595fb-7e9d-4725-a08f-3efbae89c3a8.png)
