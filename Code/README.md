This tool aims to extract data from a running QEMU-instance and create checkpoints from the data gathered.
To achieve this, it uses the QMP-protocol available in QEMU, as this should mean it works with all modern QEMU-versions without having to modifiy source-code.

To enable qmp, the following arguments need to be appended to the launch commands of QEMU:
`-qmp tcp:localhost:PORT,server`. This means the VM waits for a QMP-client to connect before it starts execution.
If this is not needed, the command `-qmp tcp:localhost:PORT,server,wait=off` may be used instead.
It appears the standard port used for this is 4444, however any available port can be used.

The communication between QEMU and any tool is realized using Telnet. There are other options, like Unix-Sockets,
however they are not as universal as Telnet and therefore weren't used.

The "standard way" to use this program is through `Checkpoint` in conjunction with `QMPInterface`.
After the QEMU-server was launched, a `QMPInterface` may be created, which needs the hostname and port to connect to.

Afterwards, a new Checkpoint can be created by calling the static method `createCheckpoint(Path location, QMPInterface qmpInterface)`
with the `QMPInterface` created before as well as the `Path` where the checkpoints should be stored at. Each checkpoint gets its own folder in this directory,
their names are generated from the timestamp of the creation.

It may be necessary to specify a `Path` for temporary files. The class `ElfDump` defaults to using the standard temp path of the system,
however `/tmp` under Linux does not seem to like these larger files, so a call to the `setTemp(Path temp)`-method may be necessary.

Each checkpoint directory contains a JSON-file with all gathered data as well as the locations of the stored memory and disk dumps.
Using the `createFollowUp(@NotNull QMPInterface qmpInterface)`-method of `Checkpoint` it is possible to create a new Checkpoint
which checks for whether the data in the files has actually changed,
and if not, it links to the old files.

It is also possible to directly use the commands to control QMP or to only extract certain desired data,
however for this it's probably better to read the JavaDoc.