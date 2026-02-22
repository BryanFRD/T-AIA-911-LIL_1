import "./global.css";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { NavigationContainer } from "@react-navigation/native";
import { createBottomTabNavigator } from "@react-navigation/bottom-tabs";
import { Mic, Bookmark } from "lucide-react-native";
import VoiceRecorderScreen from "@/screens/VoiceRecorderScreen";
import SavedTripsScreen from "@/screens/SavedTripsScreen";
import { SavedProposalsProvider, useSavedProposals } from "@/context/SavedProposalsContext";

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 2,
      staleTime: 1000 * 60 * 5,
    },
  },
});

const Tab = createBottomTabNavigator();

function TabNavigator() {
  const { savedProposals } = useSavedProposals();

  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: "#007AFF",
        tabBarInactiveTintColor: "#8E8E93",
        tabBarStyle: {
          backgroundColor: "#FFFFFF",
          borderTopColor: "#E5E5EA",
          borderTopWidth: 0.5,
          paddingTop: 6,
          height: 88,
        },
        tabBarLabelStyle: {
          fontSize: 11,
          fontWeight: "600",
        },
      }}
    >
      <Tab.Screen
        name="VoiceRecorder"
        component={VoiceRecorderScreen}
        options={{
          tabBarLabel: "Recherche",
          tabBarIcon: ({ color, size }) => <Mic size={size} color={color} />,
        }}
      />
      <Tab.Screen
        name="SavedTrips"
        component={SavedTripsScreen}
        options={{
          tabBarLabel: "Sauvegardés",
          tabBarIcon: ({ color, size }) => <Bookmark size={size} color={color} />,
          tabBarBadge: savedProposals.length > 0 ? savedProposals.length : undefined,
        }}
      />
    </Tab.Navigator>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <SavedProposalsProvider>
        <NavigationContainer>
          <TabNavigator />
        </NavigationContainer>
      </SavedProposalsProvider>
    </QueryClientProvider>
  );
}
