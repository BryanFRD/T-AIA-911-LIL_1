import React, { useState } from "react";
import { View } from "react-native";
import { StatusBar } from "expo-status-bar";
import LocationPermissionModal from "../components/modal/LocationPermissionModal";
import { Header, LocationDisplay, RecordingTimer, PlaybackControls, RecordButton, LoadingView } from "../components/voiceRecorder";
import { useVoiceRecorder } from "../hooks/useVoiceRecorder";
import { useLocation } from "../hooks/useLocation";

export default function VoiceRecorderScreen() {
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [loading, setLoading] = useState(false);

  const { isRecording, recordingUri, isPlaying, timer, toggleRecording, togglePlayback, stopAndDeleteRecording } = useVoiceRecorder();
  const { address, loadingLocation, permissionDenied, fadeAnim, slideAnim, requestLocation } = useLocation();

  return (
    <View className="flex-1 pt-24 bg-white">
      <StatusBar style="light" />
      <LocationPermissionModal visible={showPermissionModal} onClose={() => setShowPermissionModal(false)} />

      {loading ?
        <LoadingView message="Envoi de votre vocal en cours..." />
      : <>
          <View className="flex-1 justify-center items-center px-4 gap-2">
            <Header title="Travel Order Resolver App" subtitle="Voice Recorder " />

            <LocationDisplay
              address={address}
              permissionDenied={permissionDenied}
              loadingLocation={loadingLocation}
              fadeAnim={fadeAnim}
              slideAnim={slideAnim}
              onRequestLocation={requestLocation}
              onShowPermissionModal={() => setShowPermissionModal(true)}
            />
          </View>

          <View className="absolute bottom-0 left-0 right-0 ">
            {isRecording && <RecordingTimer timer={timer} />}

            {!isRecording && recordingUri && <PlaybackControls isPlaying={isPlaying} onTogglePlayback={togglePlayback} />}

            <RecordButton isRecording={isRecording} onSend={toggleRecording} onCancel={stopAndDeleteRecording} />
          </View>
        </>
      }
    </View>
  );
}
