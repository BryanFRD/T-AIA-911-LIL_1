import { View, Text, FlatList, ActivityIndicator } from 'react-native';
import { useUsers } from '../hooks/useUsers';

export default function HomeScreen() {
  const { data: users, isLoading, error } = useUsers();

  if (isLoading) {
    return (
      <View className="flex-1 bg-white items-center justify-center">
        <ActivityIndicator size="large" color="#2563eb" />
        <Text className="text-gray-600 mt-4">Loading users...</Text>
      </View>
    );
  }

  if (error) {
    return (
      <View className="flex-1 bg-white items-center justify-center">
        <Text className="text-red-600 text-lg">Error loading users</Text>
        <Text className="text-gray-600 mt-2">{error.message}</Text>
      </View>
    );
  }

  return (
    <View className="flex-1 bg-white">
      <View className="p-4 bg-blue-600">
        <Text className="text-2xl font-bold text-white">Users ({users?.length})</Text>
      </View>
      <FlatList
        data={users}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => (
          <View className="p-4 border-b border-gray-200">
            <Text className="text-lg font-semibold text-gray-900">{item.name}</Text>
            <Text className="text-sm text-gray-600">{item.email}</Text>
            <Text className="text-xs text-gray-400 mt-1">@{item.username}</Text>
          </View>
        )}
      />
    </View>
  );
}
