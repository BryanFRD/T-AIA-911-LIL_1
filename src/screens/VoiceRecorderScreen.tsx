import React, { useState } from "react";
import { View, Text } from "react-native";
import { StatusBar } from "expo-status-bar";
import LocationPermissionModal from "../components/modal/LocationPermissionModal";
import { Header, LocationDisplay, RecordingTimer, PlaybackControls, RecordButton, LoadingView } from "../components/voiceRecorder";
import { useVoiceRecorder } from "../hooks/useVoiceRecorder";
import { useLocation } from "../hooks/useLocation";
import { useTranscribeAudio } from "@/hooks/queries/useTranscription";

export default function VoiceRecorderScreen() {
  const [showPermissionModal, setShowPermissionModal] = useState(false);
  const [transcribedText, setTranscribedText] = useState<string | null>(null);

  const { isRecording, recordingUri, isPlaying, timer, startRecording, stopRecording, togglePlayback, stopAndDeleteRecording } = useVoiceRecorder();
  const { address, loadingLocation, permissionDenied, fadeAnim, slideAnim, requestLocation } = useLocation();

  const { mutate: transcribeAudio, isPending, isError, error } = useTranscribeAudio();

  const handleRecordButtonPress = async () => {
    if (isRecording) {
      const uri = await stopRecording();

      if (uri) {
        transcribeAudio(uri, {
          onSuccess: (data) => {
            setTranscribedText(data.transcribedText);
            console.log("Transcription:", data.transcribedText);
          },
          onError: (err) => {
            console.error("Failed to send audio for transcription:", err);
          },
        });
      }
    } else {
      await startRecording();
    }
  };

  return (
    <View className="flex-1 pt-24 bg-white">
      <StatusBar style="light" />
      <LocationPermissionModal visible={showPermissionModal} onClose={() => setShowPermissionModal(false)} />

      {isPending ?
        <LoadingView message="Transcription en cours..." />
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

            {transcribedText && (
              <View className="mt-4 p-4 bg-gray-100 rounded-lg w-full">
                <Text className="text-sm font-semibold text-gray-700 mb-2">Transcription:</Text>
                <Text className="text-base text-gray-900">{transcribedText}</Text>
              </View>
            )}

            {isError && (
              <View className="mt-4 p-4 bg-red-100 rounded-lg w-full">
                <Text className="text-sm font-semibold text-red-700 mb-2">Erreur {(error as any)?.response?.status || ""}:</Text>
                <Text className="text-base text-red-900 mb-2">
                  {(error as any)?.response?.data?.message || error?.message || "Impossible de transcrire l'audio"}
                </Text>
                {(error as any)?.response?.data && <Text className="text-xs text-red-700 mt-2">{JSON.stringify((error as any)?.response?.data, null, 2)}</Text>}
              </View>
            )}
          </View>

          <View className="absolute bottom-0 left-0 right-0 ">
            {isRecording && <RecordingTimer timer={timer} />}
            <RecordButton isRecording={isRecording} onSend={handleRecordButtonPress} onCancel={stopAndDeleteRecording} />
          </View>
        </>
      }
    </View>
  );
}
