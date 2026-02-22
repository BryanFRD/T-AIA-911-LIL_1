import { useMutation } from "@tanstack/react-query";
import { transcriptionApi } from "@api/endpoints/transcription.api";
import { TranscriptionResponse } from "@/interfaces/transcription.interface";

export const useTranscribeAudio = () => {
  return useMutation({
    mutationFn: (audioUri: string) => transcriptionApi.transcribeAudio(audioUri),
    onSuccess: (data: TranscriptionResponse) => {
      console.log("Transcription réussie:", data.transcription.transcribedText);
    },
    onError: (error) => {
      console.error("Erreur de transcription:", error);
    },
  });
};
