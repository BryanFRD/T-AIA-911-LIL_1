import { useMutation } from "@tanstack/react-query";
import { transcriptionApi } from "@api/endpoints/transcription.api";

export const useTranscribeAudio = () => {
  return useMutation({
    mutationFn: (audioUri: string) => transcriptionApi.transcribeAudio(audioUri),
    onSuccess: (data) => {
      console.log("Transcription réussie:", data.transcribedText);
    },
    onError: (error) => {
      console.error("Erreur de transcription:", error);
    },
  });
};
