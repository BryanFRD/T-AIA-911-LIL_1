import React, { useEffect, useRef } from "react";
import { View, Text, Animated, Easing, Dimensions } from "react-native";

interface LoadingViewProps {
  message?: string;
}

export default function LoadingView({ message = "Chargement en cours" }: LoadingViewProps) {
  const dots = [
    useRef(new Animated.Value(0)).current,
    useRef(new Animated.Value(0)).current,
    useRef(new Animated.Value(0)).current,
    useRef(new Animated.Value(0)).current,
  ];

  useEffect(() => {
    dots.forEach((dot, i) => {
      Animated.loop(
        Animated.sequence([
          Animated.delay(i * 200),
          Animated.timing(dot, {
            toValue: 1,
            duration: 500,
            easing: Easing.out(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.timing(dot, {
            toValue: 0,
            duration: 500,
            easing: Easing.in(Easing.ease),
            useNativeDriver: true,
          }),
          Animated.delay((3 - i) * 200),
        ]),
      ).start();
    });
  }, []);

  return (
    <View className="flex-1 justify-center items-center bg-white">
      <View className="items-center">
        <View className="flex-row items-center gap-3 mb-6">
          {dots.map((dot, i) => {
            const scale = dot.interpolate({
              inputRange: [0, 1],
              outputRange: [0.6, 1.2],
            });
            const opacity = dot.interpolate({
              inputRange: [0, 1],
              outputRange: [0.3, 1],
            });
            return <Animated.View key={i} style={{ transform: [{ scale }], opacity }} className={`w-2 h-2 rounded-full bg-black`} />;
          })}
        </View>

        {message && <Text className="text-gray-700 font-bold text-xl tracking-tight">{message}</Text>}
      </View>
    </View>
  );
}
