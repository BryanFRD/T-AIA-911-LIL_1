import React, { useEffect, useRef } from "react";
import { View, Text, Animated, Easing } from "react-native";
import { formatTime } from "../../utils/timeHelper";

interface RecordingTimerProps {
  timer: number;
}

export default function RecordingTimer({ timer }: RecordingTimerProps) {
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(-20)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 400,
        easing: Easing.bezier(0.4, 0.0, 0.2, 1),
        useNativeDriver: true,
      }),
      Animated.spring(slideAnim, {
        toValue: 0,
        tension: 100,
        friction: 8,
        useNativeDriver: true,
      }),
    ]).start();
  }, []);

  return (
    <Animated.View
      style={{
        opacity: fadeAnim,
        transform: [{ translateY: slideAnim }],
      }}
    >
      <View>
        <Text className="text-[#007AFF] text-center font-semibold py-2 text-xl tracking-wider">{formatTime(timer)}</Text>
      </View>
    </Animated.View>
  );
}
