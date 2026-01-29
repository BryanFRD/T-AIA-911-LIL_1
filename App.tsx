import "./global.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import VoiceRecorderScreen from "@/screens/VoiceRecorderScreen";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 1000 * 60 * 5,
    },
  },
});

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <VoiceRecorderScreen />
    </QueryClientProvider>
  );
}
