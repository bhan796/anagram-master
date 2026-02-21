export class PresenceStore {
  private readonly onlinePlayerIds = new Set<string>();

  markOnline(playerId: string): void {
    this.onlinePlayerIds.add(playerId);
  }

  markOffline(playerId: string): void {
    this.onlinePlayerIds.delete(playerId);
  }

  getCount(): number {
    return this.onlinePlayerIds.size;
  }
}
