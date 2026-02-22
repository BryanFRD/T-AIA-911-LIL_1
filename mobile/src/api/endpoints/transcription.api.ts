import { apiClient } from "../client";
import { TranscriptionResponse } from "@/interfaces/transcription.interface";
import { RequestSolution } from "@/interfaces/solution.interface";
import { SolutionResponse } from "@/interfaces/solution.interface";

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

export const requestSolutionApi = {
  requestSolution: async (data: RequestSolution): Promise<SolutionResponse> => {
    const solution = await apiClient.post<SolutionResponse>("/travel-order/find-solution", data);
    return solution.data;
  },
};
