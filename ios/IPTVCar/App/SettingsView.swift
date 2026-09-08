import SwiftUI

/// The only screen that ever handles raw credentials (Phase 3
/// requirement). Nothing entered here is logged, and the raw M3U
/// URL/password is never re-displayed once a provider is saved.
struct SettingsView: View {
    @State private var providers: [Provider] = ProviderRepository.shared.listProviders()
    @State private var showXtreamForm = false
    @State private var showM3uForm = false

    var body: some View {
        NavigationView {
            List {
                ForEach(providers) { provider in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(provider.displayName).font(.headline)
                            Text(provider.kind.rawValue.uppercased()).font(.caption).foregroundColor(.secondary)
                        }
                        Spacer()
                        Button("Remove") {
                            ProviderRepository.shared.deleteProvider(provider.id)
                            providers = ProviderRepository.shared.listProviders()
                        }
                    }
                }
                Section {
                    Button("Add Xtream Provider") { showXtreamForm = true }
                    Button("Add M3U Provider") { showM3uForm = true }
                }
            }
            .navigationTitle("Settings")
        }
        .sheet(isPresented: $showXtreamForm) {
            XtreamForm { name, url, user, pass in
                ProviderRepository.shared.addXtreamProvider(displayName: name, baseUrl: url, username: user, password: pass)
                providers = ProviderRepository.shared.listProviders()
                showXtreamForm = false
            }
        }
        .sheet(isPresented: $showM3uForm) {
            M3uForm { name, url in
                ProviderRepository.shared.addM3uProvider(displayName: name, url: url)
                providers = ProviderRepository.shared.listProviders()
                showM3uForm = false
            }
        }
    }
}

private struct XtreamForm: View {
    let onSubmit: (String, String, String, String) -> Void
    @State private var name = ""
    @State private var url = ""
    @State private var user = ""
    @State private var pass = ""

    var body: some View {
        NavigationView {
            Form {
                TextField("Display name", text: $name)
                TextField("Server URL", text: $url).keyboardType(.URL).autocapitalization(.none)
                TextField("Username", text: $user).autocapitalization(.none)
                SecureField("Password", text: $pass)
            }
            .navigationTitle("Add Xtream Provider")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onSubmit(name.isEmpty ? url : name, url, user, pass) }
                }
            }
        }
    }
}

private struct M3uForm: View {
    let onSubmit: (String, String) -> Void
    @State private var name = ""
    @State private var url = ""

    var body: some View {
        NavigationView {
            Form {
                TextField("Display name", text: $name)
                TextField("Playlist URL or file path", text: $url).keyboardType(.URL).autocapitalization(.none)
            }
            .navigationTitle("Add M3U Provider")
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { onSubmit(name.isEmpty ? url : name, url) }
                }
            }
        }
    }
}
