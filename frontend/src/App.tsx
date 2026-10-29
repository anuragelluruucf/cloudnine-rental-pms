import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth.tsx'
import { Layout } from './components/Layout.tsx'
import { DashboardPage } from './pages/DashboardPage.tsx'
import { InboxPage } from './pages/InboxPage.tsx'
import { LoginPage } from './pages/LoginPage.tsx'
import { PropertiesPage } from './pages/PropertiesPage.tsx'
import { PropertyFormPage } from './pages/PropertyFormPage.tsx'
import { PropertyPage } from './pages/PropertyPage.tsx'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route element={<Layout />}>
            <Route path="/" element={<DashboardPage />} />
            <Route path="/properties" element={<PropertiesPage />} />
            <Route path="/properties/new" element={<PropertyFormPage />} />
            <Route path="/properties/:id" element={<PropertyPage />} />
            <Route path="/properties/:id/edit" element={<PropertyFormPage />} />
            <Route path="/inbox" element={<InboxPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
