import React from "react";
import { View, Text } from "react-native";

interface HeaderProps {
  title: string;
  subtitle: string;
}

export default function Header({ title, subtitle }: HeaderProps) {
  return (
    <View className="items-center gap-2">
      <Text className="text-black font-bold text-2xl">{title}</Text>
      <Text className="text-black text-2xl">{subtitle}</Text>
    </View>
  );
}
