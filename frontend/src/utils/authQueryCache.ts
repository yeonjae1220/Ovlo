import type { QueryClient } from '@tanstack/react-query'

// Clients are registered by mounted browser providers, never shared across SSR requests.
const clients = new Set<QueryClient>()

export function registerAuthQueryClient(client: QueryClient) {
  clients.add(client)
  return () => { clients.delete(client) }
}

export function clearAuthQueryCaches() {
  for (const client of clients) {
    // Cancel before clearing so late responses cannot restore the old account's data.
    void client.cancelQueries()
    client.clear()
  }
}
