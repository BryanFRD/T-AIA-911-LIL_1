import React, { useEffect, useRef } from "react";
import { View, Animated } from "react-native";

const WaveBars = ({ isRecording }: { isRecording: boolean }) => {
  const numBars = 35;
  const animations = useRef(Array.from({ length: numBars }, () => new Animated.Value(0.3))).current;

  useEffect(() => {
    if (isRecording) {
      const animateBars = animations.map((anim, index) => {
        const randomDuration = 200 + Math.random() * 300;
        const randomDelay = index * 20;

        return Animated.loop(
          Animated.sequence([
            Animated.delay(randomDelay),
            Animated.timing(anim, {
              toValue: 0.4 + Math.random() * 0.6,
              duration: randomDuration,
              useNativeDriver: true,
            }),
            Animated.timing(anim, {
              toValue: 0.2 + Math.random() * 0.3,
              duration: randomDuration,
              useNativeDriver: true,
            }),
          ]),
        );
      });

      animateBars.forEach((anim) => anim.start());

      return () => {
        animateBars.forEach((anim) => anim.stop());
        animations.forEach((anim) => anim.setValue(0.3));
      };
    }
  }, [isRecording]);

  return (
    <View className="flex-row items-center justify-between h-8 flex-1 ml-4 mr-3">
      {animations.map((anim, index) => (
        <Animated.View key={index} className="w-[3px] bg-[#007AFF]/70 rounded-full h-7" style={{ transform: [{ scaleY: anim }] }} />
      ))}
    </View>
  );
};

export default WaveBars;
