export interface TranscriptionResponse {
  transcription: {
    transcribedText: string;
    confidence: number
    isReliable: boolean
  }
  canSkipUserConfirmation: boolean
}