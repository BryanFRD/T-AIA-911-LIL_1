import { useRef, useEffect } from "react";
import { Animated, Easing } from "react-native";

function Spinner() {
  const spin = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    Animated.loop(
      Animated.timing(spin, {
        toValue: 1,
        duration: 800,
        easing: Easing.linear,
        useNativeDriver: true,
      }),
    ).start();
  }, []);

  const rotate = spin.interpolate({
    inputRange: [0, 1],
    outputRange: ["0deg", "360deg"],
  });

  return <Animated.View style={{ transform: [{ rotate }] }} className="w-5 h-5 border-2 border-gray-300 border-t-black rounded-full" />;
}

export default Spinner;
