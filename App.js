import React, { useCallback, useEffect, useState } from "react";
import {
  ActivityIndicator,
  FlatList,
  RefreshControl,
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { StatusBar as ExpoStatusBar } from "expo-status-bar";

const API_URL =
  "https://wh-integration-assets.onrender.com/open/v1/sensor-result";

const PAGE_SIZE = 10;

function formatTimestamp(value) {
  if (!value) return "-";
  const match = String(value).match(
    /^(\\d{4})-(\\d{2})-(\\d{2})[ T](\\d{2}):(\\d{2})/
  );
  if (!match) return value;
  const [, y, m, d, h, min] = match;
  const months = [
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
  ];
  return `${d} ${months[Number(m) - 1]} ${y}, ${h}:${min}`;
}

function AlertCard({ item }) {
  return (
    <View style={styles.card}>
      <Text style={styles.timestamp}>{formatTimestamp(item.timestamp)}</Text>
      <Text style={styles.fieldCrop}>
        {item.field_id || "-"}  •  {item.crop || "-"}
      </Text>
      <Text style={styles.label}>SMS ALERT</Text>
      <Text style={styles.message}>
        {item.sms_alert_message || "No alert message"}
      </Text>
    </View>
  );
}

export default function App() {
  const [page, setPage] = useState(1);
  const [total, setTotal] = useState(0);
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState("");

  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

  const load = useCallback(async (requestedPage = page, isRefresh = false) => {
    try {
      setError("");
      if (isRefresh) setRefreshing(true);
      else setLoading(true);

      const response = await fetch(
        `${API_URL}?page=${requestedPage}&size=${PAGE_SIZE}`
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const json = await response.json();

      if (!json.success) {
        throw new Error(json.error || "API returned success=false");
      }

      setItems(Array.isArray(json.data) ? json.data : []);
      setTotal(Number(json.pagination?.total || 0));
      setPage(Number(json.pagination?.page || requestedPage));
    } catch (e) {
      setError(e?.message || "Unable to load alerts");
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [page]);

  useEffect(() => {
    load(1);
  }, []);

  const goPrevious = () => {
    if (page > 1) load(page - 1);
  };

  const goNext = () => {
    if (page < totalPages) load(page + 1);
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ExpoStatusBar style="dark" />
      <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />

      <View style={styles.header}>
        <Text style={styles.title}>Soil Sensor Alerts</Text>
        <TouchableOpacity
          style={styles.refreshButton}
          onPress={() => load(page, true)}
        >
          <Text style={styles.refreshText}>Refresh</Text>
        </TouchableOpacity>
      </View>

      {loading && !refreshing ? (
        <View style={styles.center}>
          <ActivityIndicator size="large" color="#2E7D32" />
          <Text style={styles.loadingText}>Loading alerts...</Text>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item, index) => item._id || `${item.timestamp}-${index}`}
          renderItem={({ item }) => <AlertCard item={item} />}
          contentContainerStyle={
            items.length ? styles.list : styles.emptyList
          }
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={() => load(page, true)}
              colors={["#2E7D32"]}
            />
          }
          ListEmptyComponent={
            <View style={styles.center}>
              <Text style={styles.emptyText}>
                {error || "No sensor alerts found."}
              </Text>
            </View>
          }
        />
      )}

      {error && !loading && items.length > 0 ? (
        <Text style={styles.errorText}>{error}</Text>
      ) : null}

      <View style={styles.pagination}>
        <TouchableOpacity
          style={[styles.pageButton, page <= 1 && styles.disabled]}
          disabled={page <= 1}
          onPress={goPrevious}
        >
          <Text style={styles.pageButtonText}>← Previous</Text>
        </TouchableOpacity>

        <Text style={styles.pageText}>
          {page} / {totalPages}
        </Text>

        <TouchableOpacity
          style={[styles.pageButton, page >= totalPages && styles.disabled]}
          disabled={page >= totalPages}
          onPress={goNext}
        >
          <Text style={styles.pageButtonText}>Next →</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: "#F5F7F5",
  },
  header: {
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 18,
    paddingVertical: 18,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: 1,
    borderBottomColor: "#E6EAE6",
  },
  title: {
    flex: 1,
    fontSize: 22,
    fontWeight: "800",
    color: "#1B1B1B",
  },
  refreshButton: {
    backgroundColor: "#2E7D32",
    paddingHorizontal: 14,
    paddingVertical: 9,
    borderRadius: 9,
  },
  refreshText: {
    color: "#FFFFFF",
    fontWeight: "700",
  },
  list: {
    padding: 14,
  },
  emptyList: {
    flexGrow: 1,
  },
  card: {
    backgroundColor: "#FFFFFF",
    borderRadius: 16,
    padding: 18,
    marginBottom: 13,
    borderWidth: 1,
    borderColor: "#E1E5E1",
    shadowColor: "#000000",
    shadowOpacity: 0.05,
    shadowRadius: 5,
    shadowOffset: { width: 0, height: 2 },
    elevation: 2,
  },
  timestamp: {
    color: "#666666",
    fontSize: 13,
  },
  fieldCrop: {
    color: "#1B1B1B",
    fontSize: 19,
    fontWeight: "800",
    marginTop: 7,
    marginBottom: 10,
  },
  label: {
    color: "#2E7D32",
    fontSize: 12,
    fontWeight: "800",
  },
  message: {
    color: "#333333",
    fontSize: 16,
    lineHeight: 23,
    marginTop: 7,
  },
  center: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
  },
  loadingText: {
    marginTop: 12,
    color: "#555555",
  },
  emptyText: {
    textAlign: "center",
    color: "#555555",
    fontSize: 16,
  },
  errorText: {
    color: "#B3261E",
    backgroundColor: "#FDECEA",
    paddingHorizontal: 14,
    paddingVertical: 8,
    textAlign: "center",
  },
  pagination: {
    backgroundColor: "#FFFFFF",
    paddingHorizontal: 10,
    paddingVertical: 9,
    flexDirection: "row",
    alignItems: "center",
    borderTopWidth: 1,
    borderTopColor: "#E6EAE6",
  },
  pageButton: {
    flex: 1,
    backgroundColor: "#E8F3E9",
    borderRadius: 9,
    paddingVertical: 11,
    alignItems: "center",
  },
  pageButtonText: {
    color: "#2E7D32",
    fontWeight: "700",
  },
  disabled: {
    opacity: 0.35,
  },
  pageText: {
    width: 70,
    textAlign: "center",
    color: "#333333",
    fontWeight: "700",
  },
});
