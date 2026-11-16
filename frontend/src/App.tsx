import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './components/layout/AppLayout';
import BalanceAnalyzer from './pages/BalanceAnalyzer';
import Battlefield from './pages/Battlefield';
import Dashboard from './pages/Dashboard';
import LevelEditor from './pages/LevelEditor';
import Reports from './pages/Reports';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="battlefield" element={<Battlefield />} />
          <Route path="level-editor" element={<LevelEditor />} />
          <Route path="balance-analyzer" element={<BalanceAnalyzer />} />
          <Route path="reports" element={<Reports />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}