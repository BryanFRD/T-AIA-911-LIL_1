import { useMutation } from "@tanstack/react-query";
import { TranscriptionResponse } from "@/interfaces/transcription.interface";
import { requestSolutionApi } from "@api/endpoints/transcription.api";
import { RequestSolution, SolutionResponse } from "@/interfaces/solution.interface";

export const useSolution = () => {
  return useMutation({
    mutationFn: (data: RequestSolution) => requestSolutionApi.requestSolution(data),
    onSuccess: (data: SolutionResponse) => {
      console.log("Solution reçue:", data);
    },
    onError: (error) => {
      console.error("Erreur de solution:", error);
    },
  });
};
