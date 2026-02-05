import { apiClient } from "../client";

export interface TranscriptionResponse {
  transcribedText: string;
}

export const transcriptionApi = {
  transcribeAudio: async (audioUri: string): Promise<TranscriptionResponse> => {
    const formData = new FormData();

    formData.append("audio", {
      uri: audioUri,
      type: "audio/wav",
      name: "recording.wav",
    } as any);

    const response = await apiClient.post<TranscriptionResponse>("/travel-order/transcribe", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });

    return response.data;
  },
};
