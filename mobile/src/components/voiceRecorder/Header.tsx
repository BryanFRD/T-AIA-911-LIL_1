import React from "react";
import { View, Text } from "react-native";

interface HeaderProps {
  title: string;
  subtitle: string;
}

export default function Header({ title, subtitle }: HeaderProps) {
  return (
    <View className="items-center gap-1">
      <Text className="text-black font-bold text-[28px] tracking-tight">{title}</Text>
      <Text className="text-gray-400 text-base font-normal">{subtitle}</Text>
    </View>
  );
}
