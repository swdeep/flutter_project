import 'package:flutter/material.dart';
import 'package:bubbly_camera/bubbly_camera.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Bubbly Camera',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: const MyHomePage(title: 'Bubbly Camera Demo'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  bool _isRecording = false;
  bool _isCameraOn = true;
  int _counter = 0;

  @override
  void initState() {
    super.initState();

    BubblyCamera.platformVersion.then((value) => print('Platform Version: $value'));
  }

  void _toggleCamera() async {
    setState(() {
      _isCameraOn = !_isCameraOn;
    });
    await BubblyCamera.toggleCamera;
  }

  void _startRecording() async {
    setState(() {
      _isRecording = true;
    });
    await BubblyCamera.startRecording;
  }

  void _stopRecording() async {
    setState(() {
      _isRecording = false;
    });
    await BubblyCamera.stopRecording;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text('You have pushed the button this many times:'),
            Text(
              '$_counter',
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            const SizedBox(height: 20),

            const SizedBox(height: 20),
            _isCameraOn
                ? const Text("Camera is ON")
                : const Text("Camera is OFF"),
          ],
        ),
      ),
      floatingActionButton: Row(
        mainAxisAlignment: MainAxisAlignment.end,
        children: [
          FloatingActionButton(
            onPressed: _toggleCamera,
            tooltip: 'Toggle Camera',
            child: const Icon(Icons.switch_camera),
          ),
          const SizedBox(width: 20),
          FloatingActionButton(
            onPressed: _isRecording ? _stopRecording : _startRecording,
            tooltip: _isRecording ? 'Stop Recording' : 'Start Recording',
            child: Icon(_isRecording ? Icons.stop : Icons.videocam),
          ),
        ],
      ),
    );
  }
}
