import { View, Text } from 'react-native';

export default function ProfileScreen() {
  return (
    <View className="flex-1 bg-gray-50 items-center justify-center">
      <Text className="text-4xl font-bold text-purple-600 mb-4">Profile</Text>
      <Text className="text-gray-600">Your profile information here</Text>
    </View>
  );
}
