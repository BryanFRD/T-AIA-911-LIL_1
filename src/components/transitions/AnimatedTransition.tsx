import React, { useEffect, useRef } from "react";
import { Animated, Easing } from "react-native";

interface AnimatedTransitionProps {
  isLoading: boolean;
  loadingContent: React.ReactNode;
  children: React.ReactNode;
}

export default function AnimatedTransition({ isLoading, loadingContent, children }: AnimatedTransitionProps) {
  const loadingOpacity = useRef(new Animated.Value(0)).current;
  const loadingScale = useRef(new Animated.Value(0.9)).current;
  const contentOpacity = useRef(new Animated.Value(1)).current;
  const contentTranslateY = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    if (isLoading) {
      Animated.parallel([
        Animated.timing(contentOpacity, {
          toValue: 0,
          duration: 300,
          easing: Easing.bezier(0.4, 0.0, 0.2, 1),
          useNativeDriver: true,
        }),
        Animated.timing(contentTranslateY, {
          toValue: -20,
          duration: 300,
          easing: Easing.bezier(0.4, 0.0, 0.2, 1),
          useNativeDriver: true,
        }),
      ]).start(() => {
        Animated.parallel([
          Animated.timing(loadingOpacity, {
            toValue: 1,
            duration: 400,
            easing: Easing.bezier(0.0, 0.0, 0.2, 1),
            useNativeDriver: true,
          }),
          Animated.spring(loadingScale, {
            toValue: 1,
            tension: 50,
            friction: 7,
            useNativeDriver: true,
          }),
        ]).start();
      });
    } else {
      Animated.parallel([
        Animated.timing(loadingOpacity, {
          toValue: 0,
          duration: 250,
          easing: Easing.bezier(0.4, 0.0, 1, 1),
          useNativeDriver: true,
        }),
        Animated.timing(loadingScale, {
          toValue: 0.9,
          duration: 250,
          easing: Easing.bezier(0.4, 0.0, 1, 1),
          useNativeDriver: true,
        }),
      ]).start(() => {
        Animated.parallel([
          Animated.timing(contentOpacity, {
            toValue: 1,
            duration: 400,
            easing: Easing.bezier(0.0, 0.0, 0.2, 1),
            useNativeDriver: true,
          }),
          Animated.spring(contentTranslateY, {
            toValue: 0,
            tension: 50,
            friction: 7,
            useNativeDriver: true,
          }),
        ]).start();
      });
    }
  }, [isLoading]);

  return (
    <>
      <Animated.View
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          opacity: loadingOpacity,
          transform: [{ scale: loadingScale }],
          pointerEvents: isLoading ? "auto" : "none",
        }}
      >
        {loadingContent}
      </Animated.View>
      <Animated.View
        style={{
          flex: 1,
          opacity: contentOpacity,
          transform: [{ translateY: contentTranslateY }],
        }}
      >
        {children}
      </Animated.View>
    </>
  );
}
