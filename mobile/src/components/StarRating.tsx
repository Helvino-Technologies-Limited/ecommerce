import React from "react";
import { View, TouchableOpacity, StyleSheet } from "react-native";
import { Ionicons } from "@expo/vector-icons";

interface Props {
  rating: number;
  maxStars?: number;
  size?: number;
  interactive?: boolean;
  onChange?: (rating: number) => void;
  color?: string;
}

export default function StarRating({
  rating,
  maxStars = 5,
  size = 20,
  interactive = false,
  onChange,
  color = "#f59e0b",
}: Props) {
  const stars = Array.from({ length: maxStars }, (_, i) => i + 1);

  return (
    <View style={styles.row}>
      {stars.map((s) => {
        const filled = s <= Math.round(rating);
        if (interactive) {
          return (
            <TouchableOpacity
              key={s}
              onPress={() => onChange?.(s)}
              hitSlop={{ top: 8, bottom: 8, left: 4, right: 4 }}
            >
              <Ionicons
                name={filled ? "star" : "star-outline"}
                size={size}
                color={filled ? color : "#d1d5db"}
                style={{ marginHorizontal: 1 }}
              />
            </TouchableOpacity>
          );
        }
        return (
          <Ionicons
            key={s}
            name={filled ? "star" : "star-outline"}
            size={size}
            color={filled ? color : "#d1d5db"}
            style={{ marginHorizontal: 1 }}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
  },
});
